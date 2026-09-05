package com.tommasoberlose.anotherwidget.shizuku;

interface IPrivilegedAlarmService {
    // Reserved by Shizuku for stopping a UserService. Keep this transaction code.
    void destroy() = 16777114;

    long getNextAlarmAlertTime() = 1;
}
