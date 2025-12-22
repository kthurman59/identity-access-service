package com.kevdev.iam.service;

import com.kevdev.iam.domain.RefreshToken;
import com.kevdev.iam.repo.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class RefreshTokenServiceTest {

  @Test
  void rotateReplacesTokenAndRevokesOld() {
    var repo = mock(RefreshTokenRepository.class);
    var svc = new RefreshTokenService(repo);

    String raw = RandomTokens.base64Url(48);
    String hash = callHash(svc, raw);

    var existing = RefreshToken.of("user1", hash, Instant.now().minusSeconds(10), Instant.now().plusSeconds(3600));
    when(repo.findFirstByTokenHashAndRevokedAtIsNull(hash)).thenReturn(Optional.of(existing));

    var result = svc.rotate(raw, 3600);

    assertThat(result.subject()).isEqualTo("user1");
    assertThat(result.newRefreshToken()).isNotBlank();

    var saved = ArgumentCaptor.forClass(RefreshToken.class);
    verify(repo, times(2)).save(saved.capture());
    assertThat(saved.getAllValues().get(0).getRevokedAt()).isNotNull();
    assertThat(saved.getAllValues().get(1).getTokenHash()).isNotEqualTo(hash);
  }

  private static String callHash(RefreshTokenService svc, String raw) {
    try {
      var m = RefreshTokenService.class.getDeclaredMethod("hash", String.class);
      m.setAccessible(true);
      return (String) m.invoke(svc, raw);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
}

