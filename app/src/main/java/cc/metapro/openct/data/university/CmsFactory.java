package cc.metapro.openct.data.university;

/*
 *  Copyright 2016 - 2017 OpenCT open source class table
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.support.annotation.NonNull;

import com.google.common.base.Strings;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.security.auth.login.LoginException;

import cc.metapro.openct.data.university.UniversityInfo.CMSInfo;
import cc.metapro.openct.data.university.item.EnrichedClassInfo;
import cc.metapro.openct.data.university.item.GradeInfo;
import cc.metapro.openct.utils.Constants;

public class CmsFactory extends UniversityFactory {

    private CmsURLFactory mURLFactory;

    public CmsFactory(UniversityService service, CMSInfo cmsInfo) {
        mCMSInfo = cmsInfo;
        mService = service;
        mClassTableInfo = cmsInfo.mClassTableInfo;
        mGradeTableInfo = cmsInfo.mGradeTableInfo;

        if (!cmsInfo.mCmsURL.endsWith("/")) {
            cmsInfo.mCmsURL += "/";
        }
        mURLFactory = new CmsURLFactory(cmsInfo.mCmsSys, cmsInfo.mCmsURL);
    }

    private String getTablePage(Map<String, String> loginMap, String pattern) throws Exception {
        // 登录获取用户中心页面
        String userCenter = login(loginMap);

        // 从用户中心页面解析获取课表链接
        String tableURL = null;
        Pattern contentPattern = Pattern.compile(pattern);
        Document doc = Jsoup.parse(userCenter, mURLFactory.LOGIN_URL);
        Elements links = doc.select("a");
        for (Element a : links) {
            // 根据界面上的文字提示进行判断
            if (contentPattern.matcher(a.text()).find()) {
                tableURL = a.absUrl("href");
                break;
            }
        }

        // 没有解析到课表链接
        if (Strings.isNullOrEmpty(tableURL)) {
            throw new Exception("很抱歉, 没能解析到链接\n请联系 OpenCT 开发人员添加");
        }

        // 解析到课表链接后获取课表页面
        String tablePage = mService.getPage(tableURL, mURLFactory.LOGIN_URL).execute().body();

        // 课表页面为空
        if (Strings.isNullOrEmpty(tablePage)) {
            throw new Exception("很抱歉, 没能根据链接获取到页面");
        }

        return tablePage;
    }

    /**
     * 解析用户中心页面时提供的 BaseURL 由 URLFactory 生成
     * 即使用 mURLFactory.LOGIN_URL 作为页面解析的 BaseURL
     *
     * @param loginMap - 教务网登录信息, 包含用户名, 密码, 验证码(如果需要的话)
     * @return 解析完成的课程信息
     * @throws IOException
     * @throws LoginException
     */
    @NonNull
    public List<EnrichedClassInfo>
    getClasses(Map<String, String> loginMap) throws Exception {
        // 获取课程表页面
        String tablePage = getTablePage(loginMap, mURLFactory.CLASS_URL_PATTERN);

        // 定义要被替代的符号, 解决课程信息中BR等被解析成为空格产生误解
        String toReplace = Constants.BR;

        // 各教务系统细节操作

        // 苏文
        if (Constants.NJSUWEN.equalsIgnoreCase(mCMSInfo.mCmsSys)) {
            toReplace = "◇";
        }
        // 强智
        else if (Constants.QZDATASOFT.equalsIgnoreCase(mCMSInfo.mCmsSys)) {
            // TODO: 17/1/15 获取强智教务网课表
            toReplace = "(\\-)|(" + Constants.BR + ")";
        }
        // 青果
        else if (Constants.KINGOSOFT.equalsIgnoreCase(mCMSInfo.mCmsSys)) {
            // TODO: 17/1/16 获取青果教务网课表
        }

        // 替换标识符, 生成课程
        tablePage = tablePage.replaceAll(toReplace, Constants.BR_REPLACER);
        Document doc = Jsoup.parse(tablePage);

        // 第一次匹配, 根据标准Id 获取 表格
        Elements tables = doc.select("table[id=" + mClassTableInfo.mClassTableID + "]");
        Element targetTable = tables.first();

        // 第一次匹配失败, 第二次匹配, 使用关键字匹配
        if (targetTable == null) {
            tables = doc.select("table:matches((星期)|(周)|(课程))");
            targetTable = tables.first();
        }

        // 两次匹配失败, 返回
        if (targetTable == null) {
            return new ArrayList<>(0);
        }

        // 从表格中获取课程信息
        List<Element> classes = UniversityUtils.getRawClasses(targetTable);

        return UniversityUtils.generateClasses(classes, mClassTableInfo);
    }

    /**
     * tend to get grade info page
     *
     * @param loginMap - cms user info
     * @return a list of grade info
     * @throws IOException
     * @throws LoginException
     */
    @NonNull
    public List<GradeInfo> getGrades(Map<String, String> loginMap) throws Exception {
        // 获取课程表页面
        String tablePage = getTablePage(loginMap, mURLFactory.GRADE_URL_PATTERN);

        // 定义要被替代的符号, 解决BR等被解析成为空格
        String toReplace = Constants.BR;

        if (Constants.QZDATASOFT.equalsIgnoreCase(mCMSInfo.mCmsSys)) {
            // TODO: 17/1/16 强智教务系统需要根据页面表单 Post 获取最新成绩
        } else if (Constants.ZFSOFT2008.equalsIgnoreCase(mCMSInfo.mCmsSys)) {
            // TODO: 17/1/16 正方2008教务系统需要根据页面表单 Get 获取最新成绩
        }

        tablePage = tablePage.replaceAll(toReplace, Constants.BR_REPLACER);

        return UniversityUtils.generateGrades(tablePage, mGradeTableInfo);
    }

    @Override
    protected String
    getCaptchaURL() {
        return mURLFactory.CAPTCHA_URL;
    }

    @Override
    protected String
    getLoginURL() {
        return mURLFactory.LOGIN_URL;
    }

    @Override
    protected String
    getLoginRefer() {
        return mURLFactory.LOGIN_REF;
    }

    @Override
    protected void resetURLFactory() {
        mURLFactory = new CmsURLFactory(mCMSInfo.mCmsSys, mCMSInfo.mCmsURL + dynPart + "/");
    }

    public static class GradeTableInfo {
        public int
                mClassCodeIndex,
                mClassNameIndex,
                mClassTypeIndex,
                mPointsIndex,
                mGradeSummaryIndex,
                mGradePracticeIndex,
                mGradeCommonIndex,
                mGradeMidExamIndex,
                mGradeFinalExamIndex,
                mGradeMakeupIndex;

        public String mGradeTableID;
    }

    public static class ClassTableInfo {

        public int
                mDailyClasses,
                mNameIndex,
                mTypeIndex,
                mDuringIndex,
                mPlaceIndex,
                mTimeIndex,
                mTeacherIndex,
                mClassStringCount,
                mClassLength;

        public String
                mClassTableID,
                mClassInfoStart;

        // Regular Expressions to parse class
        public String
                mNameRE, mTypeRE,
                mDuringRE, mTimeRE,
                mTeacherRE, mPlaceRE;
    }

    private static class CmsURLFactory {
        String
                LOGIN_URL, LOGIN_REF,
                CLASS_URL, GRADE_URL,
                CAPTCHA_URL, USER_CENTER_URL;

        String CLASS_URL_PATTERN, GRADE_URL_PATTERN;

        CmsURLFactory(@NonNull String cmsSys, @NonNull String cmsBaseURL) {
            if (!cmsBaseURL.endsWith("/")) {
                cmsBaseURL += "/";
            }
            // 南京苏文软件
            if (Constants.NJSUWEN.equalsIgnoreCase(cmsSys)) {
                LOGIN_URL = cmsBaseURL;
                LOGIN_REF = cmsBaseURL;
                CLASS_URL = cmsBaseURL + "public/kebiaoall.aspx";
                GRADE_URL = cmsBaseURL + "student/chengji.aspx";
                CAPTCHA_URL = cmsBaseURL + "yzm.aspx";
                CLASS_URL_PATTERN = "班级课表查询";
                GRADE_URL_PATTERN = "课程成绩查询";
            }
            // 正方教务系统 2012
            else if (Constants.ZFSOFT2012.equalsIgnoreCase(cmsSys)) {
                LOGIN_URL = cmsBaseURL;
                LOGIN_REF = cmsBaseURL;
                CAPTCHA_URL = cmsBaseURL + "CheckCode.aspx";
                CLASS_URL_PATTERN = "学生个人课表";
                GRADE_URL_PATTERN = "平时成绩查询";
            }
            // 正方教务系统 2008
            else if (Constants.ZFSOFT2008.equalsIgnoreCase(cmsSys)) {
                LOGIN_URL = cmsBaseURL;
                LOGIN_REF = cmsBaseURL;
                CAPTCHA_URL = cmsBaseURL + "CheckCode.aspx";
                CLASS_URL_PATTERN = "课程介绍查询";
                GRADE_URL_PATTERN = "学生成绩查询";
            }
            // 湖南强智科技
            else if (Constants.QZDATASOFT.equalsIgnoreCase(cmsSys)) {
                LOGIN_URL = cmsBaseURL;
                LOGIN_REF = cmsBaseURL;
                CAPTCHA_URL = cmsBaseURL + "verifycode.servlet";
                CLASS_URL = cmsBaseURL + "jsxsd/xskb/xskb_list.do";
                GRADE_URL = cmsBaseURL + "jsxsd/kscj/cjcx_query";
                USER_CENTER_URL = cmsBaseURL + "jsxsd/framework/xsMain.jsp";
            }
        }
    }

}
