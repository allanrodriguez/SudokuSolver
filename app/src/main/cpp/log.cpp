#include "core.h"

#include <android/log.h>

using namespace std;

static int convertLogPriority(LogPriority priority) {
    switch (priority) {
        case LogPriority::debug:
            return android_LogPriority::ANDROID_LOG_DEBUG;
        case LogPriority::error:
            return android_LogPriority::ANDROID_LOG_ERROR;
        case LogPriority::info:
            return android_LogPriority::ANDROID_LOG_INFO;
        case LogPriority::verbose:
            return android_LogPriority::ANDROID_LOG_VERBOSE;
        case LogPriority::warn:
            return android_LogPriority::ANDROID_LOG_WARN;
    }
}

void log(const LogPriority& priority, const string& tag, const string& message) {
    __android_log_write(convertLogPriority(priority), tag.c_str(), message.c_str());
}
