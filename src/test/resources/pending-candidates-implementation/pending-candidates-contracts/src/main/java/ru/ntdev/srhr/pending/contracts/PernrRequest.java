package ru.ntdev.srhr.pending.contracts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PernrRequest(
        @NotBlank @Size(max = 32) String pernr
) {}
