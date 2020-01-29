package com.fgtit.reader;

/**
 * Copyright (c) 2019 Farmerline LTD. All rights reserved.
 * Created by Clement Osei Tano K on 28/11/2019.
 */
public class Constants {
    /**
     * A key for the intent/bundle field for verification count, that is the number of times the
     * finger should be scanned to enroll or number of retries allowed for a search
     */
    public static final String VERIFICATION_COUNT_KEY = "verification_count";

    /**
     * A key for the intent/bundle field for the option to capture images when scanning a finger
     */
    public static final String CAPTURE_IMAGES_KEY = "capture_images";

    /**
     * A key for the intent/bundle field for the option to enrol or search for fingers
     */
    public static final String ENROL_FINGER_KEY = "enrol_fingers";

    /**
     * A key for the intent/bundle field for the different fingers to scan
     */
    public static final String FINGERS_KEY = "fingers_to_scan";
    public static final String RESPONDENT_RESULT = "respondent_result";


    /**
     * A class for constants used to represent the fingers
     */
    public static final class FINGERS {
        public static final String RIGHT_HAND_THUMB = "0";
        public static final String RIGHT_HAND_INDEX_FINGER = "1";
        public static final String RIGHT_HAND_MIDDLE_FINGER = "2";
        public static final String RIGHT_HAND_RING_FINGER = "3";
        public static final String RIGHT_HAND_PINKY = "4";
        public static final String LEFT_HAND_THUMB = "5";
        public static final String LEFT_HAND_INDEX_FINGER = "6";
        public static final String LEFT_HAND_MIDDLE_FINGER = "7";
        public static final String LEFT_HAND_RING_FINGER = "8";
        public static final String LEFT_HAND_PINKY = "9";
        public static final String ANY_HAND_FINGER = "10"; //A generic constant for any finger
        public static final String ALL_FINGERS = "0-1-2-3-4-4-6-7-8-9";
    }
}
