package eaglechat.eaglechat;

import junit.framework.TestCase;

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;

public class PeregrineTest extends TestCase {

    private Peregrine mPeregrine;

    public void setUp() throws Exception {
        super.setUp();
        mPeregrine = new Peregrine();
    }

    public void tearDown() throws Exception {
        mPeregrine = null;
    }

    public void testOnData() throws Exception {
        mPeregrine.onData("Unterminated message");
        assertNull(mPeregrine.mQueue.peek());
        mPeregrine.onData("\n");
        assertEquals(mPeregrine.mQueue.isEmpty(), false);
        assertEquals("Unterminated message", mPeregrine.mQueue.peek());

        for (int i = 0; i < 255; ++i) {
            mPeregrine.onData("Unterminated message");
            mPeregrine.onData("\n");
        }

        while (!mPeregrine.mQueue.isEmpty()) {
            assertEquals("Unterminated message", mPeregrine.mQueue.poll());
        }
    }

    public void testFormatSendMessage() throws Exception {

        String msg = new String(mPeregrine.formatSendMessage(10, "This is a message"));

        String test = "s:10:This is a message\n";

        assertEquals(test, msg);
    }

    public void testFormatSendPublicKey() throws Exception {

        byte[] key = new byte[32];

        for (int i = 0; i < key.length; ++i) {
            key[i] = (byte) (i + 65);
        }

        String msg = new String(mPeregrine.formatSendPublicKey(10, key));

        String test = "p:10:" + new String(key) + "\n";

        assertEquals(test, msg);
    }

    public void testGetStatus() throws Exception {

        Promise<Integer, String, String> statusPromise = mPeregrine.requestStatus();

        final Integer status = 1;

        mPeregrine.onData(status.toString() + "\n");

        statusPromise.done(new DoneCallback<Integer>() {
            @Override
            public void onDone(Integer result) {
                assertEquals("Should resolve to 1", status, result);
            }
        });

    }

    public void testGetStatusFail() throws Exception {

        Promise<Integer, String, String>
                statusPromise = mPeregrine.requestStatus()
                .done(new DoneCallback<Integer>() {
                    @Override
                    public void onDone(Integer result) {
                        assertFalse("The success handler should not have run.", true);
                    }
                }).fail(new FailCallback<String>() {
                    @Override
                    public void onFail(String result) {
                        assertEquals("This promise failed", "FAIL", Peregrine.FAIL);
                    }
                });

        mPeregrine.onData(Peregrine.FAIL + "\n");

    }

    public void testGetStatusTimeout() throws Exception {

        Promise<Integer, String, String>
                statusPromise = mPeregrine.requestStatus()
                .done(new DoneCallback<Integer>() {
                    @Override
                    public void onDone(Integer result) {
                        assertFalse("The success handler should not have run.", true);
                    }
                }).fail(new FailCallback<String>() {
                    @Override
                    public void onFail(String result) {
                        assertEquals("This promise failed", true, false);
                    }
                });

        // mPeregrine.onData(Peregrine.FAIL + "\n");

    }
}