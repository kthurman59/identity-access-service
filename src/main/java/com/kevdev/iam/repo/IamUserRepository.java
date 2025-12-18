package com.kevdev.iam.repo;

import com.kevdev.iam.domain.IamUser;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface IamUserRepository extends JpaRepository<IamUser, UUID> {}

