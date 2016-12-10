//
// Created by root on 11/13/16.
//

#ifndef AUGMENTEDSTUFF_AUGMENTER_HPP
#define AUGMENTEDSTUFF_AUGMENTER_HPP

#include <jni.h>
#include <errno.h>
#include <string>

#include <android/sensor.h>
#include <android/log.h>

#include <boost/lexical_cast.hpp>
#include <boost/thread.hpp>
#include <boost/chrono.hpp>
#include <boost/date_time.hpp>
#include <boost/date_time/posix_time/posix_time.hpp>


#include <opencv2/core/core.hpp>
#include <opencv2/imgproc/imgproc.hpp>
#include <opencv2/imgcodecs/imgcodecs.hpp>
#include <opencv2/photo/photo.hpp>
#include <opencv2/video/video.hpp>
#include <opencv2/videoio/videoio.hpp>
#include <opencv2/videostab/stabilizer.hpp>

#define APPNAME "Augmented"
#define LOGPRINT(...) ((void)__android_log_print(ANDROID_LOG_INFO, APPNAME, __VA_ARGS__))
#define TIMESSEC(TIMES) ((1000L/TIMES) * 1000)

#endif //AUGMENTEDSTUFF_AUGMENTER_HPP
