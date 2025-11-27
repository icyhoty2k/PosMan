package net.silver.utils;

import java.util.Timer;
import java.util.TimerTask;

public class UpdateTimeDate implements Runnable {
  int millisInAMinute = 60000;// 60 000 milliseconds in one minute
  long time = System.currentTimeMillis();
  Timer timer = new Timer();
  TimerTask clockUpdateTask = new TimerTask() {
    @Override public void run() {

    }
  };

  @Override public void run() {
    timer.scheduleAtFixedRate(clockUpdateTask, time % millisInAMinute, millisInAMinute);
  }
}
