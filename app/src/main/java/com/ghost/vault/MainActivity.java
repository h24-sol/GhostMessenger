package com.ghost.vault;

import android.Manifest;
import android.os.Bundle;
import android.util.Base64;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.nearby.Nearby;
import com.google.android.gms.nearby.connection.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {
    private final String SERVICE_ID = "com.ghost.p2p.v1";
    private ConnectionsClient client;
    private String remoteId;
    private TextView chatBox, status;
    private EditText msgInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        chatBox = findViewById(R.id.chatBox);
        status = findViewById(R.id.status);
        msgInput = findViewById(R.id.msgInput);
        client = Nearby.getConnectionsClient(this);

        requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT}, 1);

        startGhostNetwork();

        findViewById(R.id.sendBtn).setOnClickListener(v -> {
            String raw = msgInput.getText().toString();
            if (!raw.isEmpty() && remoteId != null) {
                String encrypted = Base64.encodeToString(raw.getBytes(), Base64.DEFAULT); 
                client.sendPayload(remoteId, Payload.fromBytes(encrypted.getBytes()));
                chatBox.append("\n[YOU]: " + raw);
                msgInput.setText("");
            }
        });
    }

    private void startGhostNetwork() {
        client.startAdvertising("User", SERVICE_ID, connectionCallback, new AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build());
        client.startDiscovery(SERVICE_ID, discoveryCallback, new DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build());
        status.setText("STATUS: SCANNING FOR PEERS...");
    }

    private final ConnectionLifecycleCallback connectionCallback = new ConnectionLifecycleCallback() {
        @Override
        public void onConnectionInitiated(String id, ConnectionInfo info) {
            client.acceptConnection(id, payloadCallback);
        }
        @Override
        public void onConnectionResult(String id, ConnectionResolution res) {
            if (res.getStatus().isSuccess()) {
                remoteId = id;
                status.setText("STATUS: CONNECTED SECURELY");
                status.setTextColor(0xFF00FF00);
            }
        }
        @Override public void onDisconnected(String id) { status.setText("STATUS: DISCONNECTED"); startGhostNetwork(); }
    };

    private final EndpointDiscoveryCallback discoveryCallback = new EndpointDiscoveryCallback() {
        @Override public void onEndpointFound(String id, DiscoveredEndpointInfo info) { client.requestConnection("User", id, connectionCallback); }
        @Override public void onEndpointLost(String id) {}
    };

    private final PayloadCallback payloadCallback = new PayloadCallback() {
        @Override
        public void onPayloadReceived(String id, Payload payload) {
            String secureData = new String(payload.asBytes(), StandardCharsets.UTF_8);
            String decrypted = new String(Base64.decode(secureData, Base64.DEFAULT));
            chatBox.append("\n[GHOST]: " + decrypted);
        }
        @Override public void onPayloadTransferUpdate(String id, PayloadTransferUpdate update) {}
    };
}
