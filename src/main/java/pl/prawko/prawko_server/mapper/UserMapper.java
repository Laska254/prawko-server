package pl.prawko.prawko_server.mapper;

import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.model.User;

import java.time.Clock;

/**
 * This class is responsible for mapping {@link RegisterDto} to {@link User} entity.
 * Uses MapStruct processor to generate {@link UserMapperImpl}.
 * <p>
 * The mapper is registered as a Spring Component, so it can be injected into services or other components that require user mapping functionality.
 * It's an abstract class rather than an interface to get the {@link Clock}, which is injected through a setter, as the
 * generated implementation has only the default constructor.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class UserMapper {

    private Clock clock;

    @Autowired
    public void setClock(final Clock clock) {
        this.clock = clock;
    }

    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "exams", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "pendingEmail", ignore = true)
    @Mapping(target = "passwordResetTokenHash", ignore = true)
    @Mapping(target = "passwordResetTokenExpires", ignore = true)
    @Mapping(target = "verificationTokenHash", ignore = true)
    @Mapping(target = "verificationTokenExpires", ignore = true)
    @Mapping(target = "currentEmailTokenHash", ignore = true)
    @Mapping(target = "enabled", constant = "false")
    public abstract User fromDto(RegisterDto registerDto);

    @Mapping(target = "pendingEmail", source = "user", qualifiedByName = "activePendingEmail")
    @Mapping(target = "awaitingNewEmailConfirmation", source = "user", qualifiedByName = "awaitingNewEmailConfirmation")
    @Mapping(target = "awaitingCurrentEmailConfirmation", source = "user",
            qualifiedByName = "awaitingCurrentEmailConfirmation")
    public abstract UserDto toDto(User user);

    /**
     * The pending email is stale once its verification link expired, nothing clears it until the next change.
     */
    @Named("activePendingEmail")
    protected @Nullable String activePendingEmail(final User user) {
        final var expires = user.getVerificationTokenExpires();
        return expires != null && expires.isAfter(clock.instant()) ? user.getPendingEmail() : null;
    }

    @Named("awaitingNewEmailConfirmation")
    protected boolean awaitingNewEmailConfirmation(final User user) {
        return activePendingEmail(user) != null && user.getVerificationTokenHash() != null;
    }

    @Named("awaitingCurrentEmailConfirmation")
    protected boolean awaitingCurrentEmailConfirmation(final User user) {
        return activePendingEmail(user) != null && user.getCurrentEmailTokenHash() != null;
    }

}
