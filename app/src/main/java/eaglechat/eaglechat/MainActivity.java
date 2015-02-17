package eaglechat.eaglechat;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import java.security.Provider;
import java.security.Security;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

public class MainActivity extends Activity {
    private final int STATE_LAUNCH_LIST_ACTIVITY = 0;
    private final int STATE_LAUNCH_CONTACTS_ACTIVITY = 1;

    private int mState = STATE_LAUNCH_CONTACTS_ACTIVITY;

    /*static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }
    */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        switch (mState) {
            case STATE_LAUNCH_LIST_ACTIVITY:
                handleLaunchListActivity();
                break;
            case STATE_LAUNCH_CONTACTS_ACTIVITY:
                handleLaunchContactsActivity();
            default:
                finish();
                break;
        }


    }

    private void handleLaunchContactsActivity() {
        Intent activityIntent = new Intent(this, ContactsActivity.class);
        startActivity(activityIntent);
        finish();
    }

    private void handleLaunchListActivity() {
        Intent activityIntent = new Intent(this, ConversationActivity.class);
        startActivity(activityIntent);
        finish();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void listSupportedAlgorithms() {
        String result = "";

        // get all the providers
        Provider[] providers = Security.getProviders();

        for (int p = 0; p < providers.length; p++) {
            // get all service types for a specific provider
            Set<Object> ks = providers[p].keySet();
            Set<String> servicetypes = new TreeSet<String>();
            for (Iterator<Object> it = ks.iterator(); it.hasNext(); ) {
                String k = it.next().toString();
                k = k.split(" ")[0];
                if (k.startsWith("Alg.Alias."))
                    k = k.substring(10);

                servicetypes.add(k.substring(0, k.indexOf('.')));
            }

            // get all algorithms for a specific service type
            int s = 1;
            for (Iterator<String> its = servicetypes.iterator(); its.hasNext(); ) {
                String stype = its.next();
                Set<String> algorithms = new TreeSet<String>();
                for (Iterator<Object> it = ks.iterator(); it.hasNext(); ) {
                    String k = it.next().toString();
                    k = k.split(" ")[0];
                    if (k.startsWith(stype + "."))
                        algorithms.add(k.substring(stype.length() + 1));
                    else if (k.startsWith("Alg.Alias." + stype + "."))
                        algorithms.add(k.substring(stype.length() + 11));
                }

                int a = 1;
                for (Iterator<String> ita = algorithms.iterator(); ita.hasNext(); ) {
                    result += ("[P#" + (p + 1) + ":" + providers[p].getName() + "]" +
                            "[S#" + s + ":" + stype + "]" +
                            "[A#" + a + ":" + ita.next() + "]\n");
                    a++;
                }

                s++;
            }
        }

        for (String s : result.split("\n")) {
            Log.d("EC", s);
        }

    }
}
