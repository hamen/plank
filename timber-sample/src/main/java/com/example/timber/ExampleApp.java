package com.example.timber;

import android.app.Application;
import android.util.Log;
import timber.log.Plank;

import static timber.log.Plank.DebugTree;

public class ExampleApp extends Application {
  @Override public void onCreate() {
    super.onCreate();

    if (BuildConfig.DEBUG) {
      Plank.plant(new DebugTree());
    } else {
      Plank.plant(new CrashReportingTree());
    }
  }

  /** A tree which logs important information for crash reporting. */
  private static class CrashReportingTree extends Plank.Tree {
    @Override protected void log(int priority, String tag, String message, Throwable t) {
      if (priority == Log.VERBOSE || priority == Log.DEBUG) {
        return;
      }

      FakeCrashLibrary.log(priority, tag, message);

      if (t != null) {
        if (priority == Log.ERROR) {
          FakeCrashLibrary.logError(t);
        } else if (priority == Log.WARN) {
          FakeCrashLibrary.logWarning(t);
        }
      }
    }
  }
}
