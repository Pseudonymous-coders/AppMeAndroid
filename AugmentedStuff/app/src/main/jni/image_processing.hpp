//
// Created by root on 11/15/16.
//

#ifndef AUGMENTED_IMAGE_PROCESSING_HPP
#define AUGMENTED_IMAGE_PROCESSING_HPP

#include "augmenter.hpp"

#define FPSPULL 15
#define REFRESHDELAY 300
#define MAXTRIES 50

class image_processing {
public:
    static void imageThreadUpdate();
    static void waitMillis(const unsigned long);
    static const unsigned int targetMillis = static_cast<unsigned int>( ((float) 1 /
            (float) FPSPULL) * 1000.0f);
    static cv::Mat *currentFrame;
    static volatile bool runLoop, goodImage;
    static unsigned char attemptTries;
    static boost::posix_time::ptime lastTime;
};

#endif //AUGMENTEDSTUFF_IMAGE_PROCESSING_HPP
