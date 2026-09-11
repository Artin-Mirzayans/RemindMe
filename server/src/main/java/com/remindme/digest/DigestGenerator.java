package com.remindme.digest;

import java.time.Instant;

public interface DigestGenerator {

    // generates a digest covering the rest of today and all of tomorrow, relative to now
    DailyDigest generate(Instant now);
}
