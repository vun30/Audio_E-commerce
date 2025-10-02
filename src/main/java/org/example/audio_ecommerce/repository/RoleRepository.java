package org.example.audio_ecommerce.repository;


import org.example.audio_ecommerce.entity.Role;
import org.example.audio_ecommerce.entity.Enum.RoleEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    Role findByName(RoleEnum roleEnum);
}
