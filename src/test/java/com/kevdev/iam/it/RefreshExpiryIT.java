package com.kevdev.iam.it;

import java.time.Duration;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshExpiryIT {

  @Test
  void durationSleepConversionCompiles() throws Exception {
    Duration ttl = Duration.ofSeconds(1);
    Thread.sleep(ttl.toMillis()); // convert Duration -> long millis
    assertThat(ttl.getSeconds()).isEqualTo(1L);
  }
}

