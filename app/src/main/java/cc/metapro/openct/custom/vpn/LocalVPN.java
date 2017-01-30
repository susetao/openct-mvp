//package cc.metapro.openct.custom.vpn;
//
///*
// *  Copyright 2016 - 2017 OpenCT open source class table
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.net.VpnService;
//import android.os.Bundle;
//import android.support.v4.content.LocalBroadcastManager;
//import android.support.v7.app.AppCompatActivity;
//import android.view.View;
//import android.widget.Button;
//
//import cc.metapro.openct.R;
//
//
//public class LocalVPN extends AppCompatActivity {
//    private static final int VPN_REQUEST_CODE = 0x0F;
//
//    private boolean waitingForVPNStart;
//
//    private BroadcastReceiver vpnStateReceiver = new BroadcastReceiver() {
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            if (MyVpnService.BROADCAST_VPN_STATE.equals(intent.getAction())) {
//                if (intent.getBooleanExtra("running", false))
//                    waitingForVPNStart = false;
//            }
//        }
//    };
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_local_vpn);
//        final Button vpnButton = (Button) findViewById(R.id.vpn);
//        vpnButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                startVPN();
//            }
//        });
//        waitingForVPNStart = false;
//        LocalBroadcastManager.getInstance(this).registerReceiver(vpnStateReceiver,
//                new IntentFilter(MyVpnService.BROADCAST_VPN_STATE));
//    }
//
//    private void startVPN() {
//        Intent vpnIntent = VpnService.prepare(this);
//        if (vpnIntent != null)
//            startActivityForResult(vpnIntent, VPN_REQUEST_CODE);
//        else
//            onActivityResult(VPN_REQUEST_CODE, RESULT_OK, null);
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == VPN_REQUEST_CODE && resultCode == RESULT_OK) {
//            waitingForVPNStart = true;
//            startService(new Intent(this, MyVpnService.class));
//            enableButton(false);
//        }
//    }
//
//    @Override
//    protected void onResume() {
//        super.onResume();
//
//        enableButton(!waitingForVPNStart && !MyVpnService.isRunning());
//    }
//
//    private void enableButton(boolean enable) {
//        final Button vpnButton = (Button) findViewById(R.id.vpn);
//        if (enable) {
//            vpnButton.setEnabled(true);
//            vpnButton.setText(R.string.start_vpn);
//        } else {
//            vpnButton.setEnabled(false);
//            vpnButton.setText(R.string.stop_vpn);
//        }
//    }
//}
