package fr.maxlego08.essentials.api.dto;

import java.util.Date;
import java.util.UUID;

public record PlayerAddressDTO(UUID unique_id, String address, Date first_seen, Date last_seen) {
}
