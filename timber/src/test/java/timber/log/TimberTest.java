package timber.log;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;

import android.util.Log;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static junit.framework.Assert.fail;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.robolectric.shadows.ShadowLog.LogItem;

@RunWith(RobolectricTestRunner.class) //
@Config(manifest = Config.NONE)
public class TimberTest {
  @Before @After public void setUpAndTearDown() {
    Plank.uprootAll();
  }

  // NOTE: This class references the line number. Keep it at the top so it does not change.
  @Test public void debugTreeCanAlterCreatedTag() {
    Plank.plant(new Plank.DebugTree() {
      @Override protected String createStackElementTag(StackTraceElement element) {
        return super.createStackElementTag(element) + ':' + element.getLineNumber();
      }
    });

    Plank.d("Test");

    assertLog()
        .hasDebugMessage("TimberTest:38", "Test")
        .hasNoMoreMessages();
  }

  @Test public void recursion() {
    Plank.Tree timber = Plank.asTree();
    try {
      Plank.plant(timber);
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessage("Cannot plant Timber into itself.");
    }
  }

  @Test public void nullTree() {
    try {
      Plank.plant(null);
      fail();
    } catch (NullPointerException e) {
      assertThat(e).hasMessage("tree == null");
    }
  }

  @Test public void forestReturnsAllPlanted() {
    Plank.DebugTree tree1 = new Plank.DebugTree();
    Plank.DebugTree tree2 = new Plank.DebugTree();
    Plank.plant(tree1);
    Plank.plant(tree2);

    assertThat(Plank.forest()).containsExactly(tree1, tree2);
  }

  @Test public void uprootThrowsIfMissing() {
    try {
      Plank.uproot(new Plank.DebugTree());
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e).hasMessageStartingWith("Cannot uproot tree which is not planted: ");
    }
  }

  @Test public void uprootRemovesTree() {
    Plank.DebugTree tree1 = new Plank.DebugTree();
    Plank.DebugTree tree2 = new Plank.DebugTree();
    Plank.plant(tree1);
    Plank.plant(tree2);
    Plank.d("First");
    Plank.uproot(tree1);
    Plank.d("Second");

    assertLog()
        .hasDebugMessage("TimberTest", "First")
        .hasDebugMessage("TimberTest", "First")
        .hasDebugMessage("TimberTest", "Second")
        .hasNoMoreMessages();
  }

  @Test public void uprootAllRemovesAll() {
    Plank.DebugTree tree1 = new Plank.DebugTree();
    Plank.DebugTree tree2 = new Plank.DebugTree();
    Plank.plant(tree1);
    Plank.plant(tree2);
    Plank.d("First");
    Plank.uprootAll();
    Plank.d("Second");

    assertLog()
        .hasDebugMessage("TimberTest", "First")
        .hasDebugMessage("TimberTest", "First")
        .hasNoMoreMessages();
  }

  @Test public void noArgsDoesNotFormat() {
    Plank.plant(new Plank.DebugTree());
    Plank.d("te%st");

    assertLog()
        .hasDebugMessage("TimberTest", "te%st")
        .hasNoMoreMessages();
  }

  @Test public void debugTreeTagGeneration() {
    Plank.plant(new Plank.DebugTree());
    Plank.d("Hello, world!");

    assertLog()
        .hasDebugMessage("TimberTest", "Hello, world!")
        .hasNoMoreMessages();
  }

  @Test public void debugTreeTagGenerationStripsAnonymousClassMarker() {
    Plank.plant(new Plank.DebugTree());
    new Runnable() {
      @Override public void run() {
        Plank.d("Hello, world!");

        new Runnable() {
          @Override public void run() {
            Plank.d("Hello, world!");
          }
        }.run();
      }
    }.run();

    assertLog()
        .hasDebugMessage("TimberTest", "Hello, world!")
        .hasDebugMessage("TimberTest", "Hello, world!")
        .hasNoMoreMessages();
  }

  @Test public void debugTreeCustomTag() {
    Plank.plant(new Plank.DebugTree());
    Plank.tag("Custom").d("Hello, world!");

    assertLog()
        .hasDebugMessage("Custom", "Hello, world!")
        .hasNoMoreMessages();
  }

  @Test public void messageWithException() {
    Plank.plant(new Plank.DebugTree());
    NullPointerException datThrowable = new NullPointerException();
    Plank.e(datThrowable, "OMFG!");

    assertExceptionLogged("OMFG!", "java.lang.NullPointerException");
  }

  @Test public void exceptionFromSpawnedThread() throws InterruptedException {
    Plank.plant(new Plank.DebugTree());
    final NullPointerException datThrowable = new NullPointerException();
    final CountDownLatch latch = new CountDownLatch(1);
    new Thread() {
      @Override public void run() {
        Plank.e(datThrowable, "OMFG!");
        latch.countDown();
      }
    }.run();
    latch.await();
    assertExceptionLogged("OMFG!", "java.lang.NullPointerException");
  }

  @Test public void nullMessageWithThrowable() {
    Plank.plant(new Plank.DebugTree());
    final NullPointerException datThrowable = new NullPointerException();
    Plank.e(datThrowable, null);

    assertExceptionLogged("", "java.lang.NullPointerException");
  }

  @Test public void chunkAcrossNewlinesAndLimit() {
    Plank.plant(new Plank.DebugTree());
    Plank.d(repeat('a', 3000) + '\n' + repeat('b', 6000) + '\n' + repeat('c', 3000));

    assertLog()
        .hasDebugMessage("TimberTest", repeat('a', 3000))
        .hasDebugMessage("TimberTest", repeat('b', 4000))
        .hasDebugMessage("TimberTest", repeat('b', 2000))
        .hasDebugMessage("TimberTest", repeat('c', 3000))
        .hasNoMoreMessages();
  }

  @Test public void nullMessageWithoutThrowable() {
    Plank.plant(new Plank.DebugTree());
    Plank.d(null);

    assertLog().hasNoMoreMessages();
  }

  @Test public void logMessageCallback() {
    final List<String> logs = new ArrayList<String>();
    Plank.plant(new Plank.DebugTree() {
      @Override protected void log(int priority, String tag, String message, Throwable t) {
        logs.add(priority + " " + tag + " " + message);
      }
    });

    Plank.v("Verbose");
    Plank.tag("Custom").v("Verbose");
    Plank.d("Debug");
    Plank.tag("Custom").d("Debug");
    Plank.i("Info");
    Plank.tag("Custom").i("Info");
    Plank.w("Warn");
    Plank.tag("Custom").w("Warn");
    Plank.e("Error");
    Plank.tag("Custom").e("Error");
    Plank.wtf("Assert");
    Plank.tag("Custom").wtf("Assert");

    assertThat(logs).containsExactly( //
        "2 TimberTest Verbose", //
        "2 Custom Verbose", //
        "3 TimberTest Debug", //
        "3 Custom Debug", //
        "4 TimberTest Info", //
        "4 Custom Info", //
        "5 TimberTest Warn", //
        "5 Custom Warn", //
        "6 TimberTest Error", //
        "6 Custom Error", //
        "7 TimberTest Assert", //
        "7 Custom Assert" //
    );
  }

  @Test public void logAtSpecifiedPriority() {
    Plank.plant(new Plank.DebugTree());

    Plank.log(Log.VERBOSE, "Hello, World!");
    Plank.log(Log.DEBUG, "Hello, World!");
    Plank.log(Log.INFO, "Hello, World!");
    Plank.log(Log.WARN, "Hello, World!");
    Plank.log(Log.ERROR, "Hello, World!");
    Plank.log(Log.ASSERT, "Hello, World!");

    assertLog()
        .hasVerboseMessage("TimberTest", "Hello, World!")
        .hasDebugMessage("TimberTest", "Hello, World!")
        .hasInfoMessage("TimberTest", "Hello, World!")
        .hasWarnMessage("TimberTest", "Hello, World!")
        .hasErrorMessage("TimberTest", "Hello, World!")
        .hasAssertMessage("TimberTest", "Hello, World!")
        .hasNoMoreMessages();
  }

  @Test public void formatting() {
    Plank.plant(new Plank.DebugTree());
    Plank.v("Hello, %s!", "World");
    Plank.d("Hello, %s!", "World");
    Plank.i("Hello, %s!", "World");
    Plank.w("Hello, %s!", "World");
    Plank.e("Hello, %s!", "World");
    Plank.wtf("Hello, %s!", "World");

    assertLog()
        .hasVerboseMessage("TimberTest", "Hello, World!")
        .hasDebugMessage("TimberTest", "Hello, World!")
        .hasInfoMessage("TimberTest", "Hello, World!")
        .hasWarnMessage("TimberTest", "Hello, World!")
        .hasErrorMessage("TimberTest", "Hello, World!")
        .hasAssertMessage("TimberTest", "Hello, World!")
        .hasNoMoreMessages();
  }

  @Test public void isLoggableControlsLogging() {
    Plank.plant(new Plank.DebugTree() {
      @Override protected boolean isLoggable(int priority) {
        return priority == Log.INFO;
      }
    });
    Plank.v("Hello, World!");
    Plank.d("Hello, World!");
    Plank.i("Hello, World!");
    Plank.w("Hello, World!");
    Plank.e("Hello, World!");
    Plank.wtf("Hello, World!");

    assertLog()
        .hasInfoMessage("TimberTest", "Hello, World!")
        .hasNoMoreMessages();
  }

  @Test public void logsUnknownHostExceptions() {
    Plank.plant(new Plank.DebugTree());
    Plank.e(new UnknownHostException(), null);

    assertExceptionLogged("", "UnknownHostException");
  }

  private static String repeat(char c, int number) {
    char[] data = new char[number];
    Arrays.fill(data, c);
    return new String(data);
  }

  private static void assertExceptionLogged(String message, String exceptionClassname) {
    List<LogItem> logs = ShadowLog.getLogs();
    assertThat(logs).hasSize(1);
    LogItem log = logs.get(0);
    assertThat(log.type).isEqualTo(Log.ERROR);
    assertThat(log.tag).isEqualTo("TimberTest");
    assertThat(log.msg).startsWith(message);
    assertThat(log.msg).contains(exceptionClassname);
    // We use a low-level primitive that Robolectric doesn't populate.
    assertThat(log.throwable).isNull();
  }

  private static LogAssert assertLog() {
    return new LogAssert(ShadowLog.getLogs());
  }

  private static final class LogAssert {
    private final List<LogItem> items;
    private int index = 0;

    private LogAssert(List<LogItem> items) {
      this.items = items;
    }

    public LogAssert hasVerboseMessage(String tag, String message) {
      return hasMessage(Log.VERBOSE, tag, message);
    }

    public LogAssert hasDebugMessage(String tag, String message) {
      return hasMessage(Log.DEBUG, tag, message);
    }

    public LogAssert hasInfoMessage(String tag, String message) {
      return hasMessage(Log.INFO, tag, message);
    }

    public LogAssert hasWarnMessage(String tag, String message) {
      return hasMessage(Log.WARN, tag, message);
    }

    public LogAssert hasErrorMessage(String tag, String message) {
      return hasMessage(Log.ERROR, tag, message);
    }

    public LogAssert hasAssertMessage(String tag, String message) {
      return hasMessage(Log.ASSERT, tag, message);
    }

    private LogAssert hasMessage(int priority, String tag, String message) {
      LogItem item = items.get(index++);
      assertThat(item.type).isEqualTo(priority);
      assertThat(item.tag).isEqualTo(tag);
      assertThat(item.msg).isEqualTo(message);
      return this;
    }

    public void hasNoMoreMessages() {
      assertThat(items).hasSize(index);
    }
  }
}
