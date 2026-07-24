package ru.ntdev.srhr.pending.contracts;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PernrRequestEnvelope(@NotNull @Valid PernrRequest request) {}
