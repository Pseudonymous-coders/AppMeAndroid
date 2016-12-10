//
// Created by root on 11/15/16.
//

#include "image_processing.hpp"

volatile bool image_processing::runLoop = true;
volatile bool image_processing::goodImage = false;
unsigned char image_processing::attemptTries = 0;
boost::posix_time::ptime image_processing::lastTime;
cv::Mat *image_processing::currentFrame;

void image_processing::waitMillis(const unsigned long millis) {
    boost::this_thread::sleep_for(boost::chrono::milliseconds(millis));
}

void image_processing::imageThreadUpdate() {
    cv::VideoCapture camera;

    camera.open(0); //Open capture on android

    image_processing::attemptTries = 0; //Reset counter to 0
    image_processing::runLoop = true;
    image_processing::goodImage = false;

    while(!camera.isOpened() && image_processing::attemptTries < MAXTRIES) {
        LOGPRINT("Camera not opened, attempting again");
        image_processing::waitMillis(REFRESHDELAY);
        image_processing::attemptTries++;
    }

    camera.set(cv::CAP_PROP_FRAME_WIDTH, 640);
    camera.set(cv::CAP_PROP_FRAME_HEIGHT, 480);


    boost::mutex mutex;

    image_processing::lastTime = boost::posix_time::second_clock::local_time();

    while(image_processing::runLoop) {
        {
            boost::mutex::scoped_lock lock(mutex);
            camera.read(*image_processing::currentFrame);
            if(image_processing::currentFrame->empty()) {
                LOGPRINT("PULLED EMPTY IMAGE!");
                image_processing::goodImage = false;
            } else {
                LOGPRINT("GOT IMAGE!");
                image_processing::goodImage = true;
            }
        }

        boost::posix_time::ptime currentTime = boost::posix_time::second_clock::local_time();
        boost::posix_time::time_duration difference = (currentTime - image_processing::lastTime);

        long millisLast = difference.total_milliseconds();

        //Make sure that we aren't eating too much cpu and monitor fps to our target fps
        if(millisLast < image_processing::targetMillis) {
            long deltaDelay = image_processing::targetMillis - millisLast;
            image_processing::waitMillis((const unsigned long) deltaDelay);
        }

        image_processing::lastTime = currentTime; //Test for previous time
    }
}

extern "C" {
    JNIEXPORT void JNICALL
    Java_tk_pseudonymous_augmented_MainActivity_processFrame(JNIEnv *env, jobject thiz,
                                                             jlong frameLoadPointer) {
        try {
            image_processing::currentFrame = (cv::Mat*) frameLoadPointer; //Frame pointer
            if(!image_processing::currentFrame->empty()) image_processing::goodImage = true;
            //LOGPRINT("GOT IMAGE FRAME!!! %d", (int) image_processing::goodImage);
        } catch (...) {
            LOGPRINT("Failed loading image from JNI frame");
            image_processing::goodImage = false;
        }
    }
}