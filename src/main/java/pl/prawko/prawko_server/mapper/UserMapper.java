package pl.prawko.prawko_server.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import pl.prawko.prawko_server.dto.RegisterDto;
import pl.prawko.prawko_server.dto.UserDto;
import pl.prawko.prawko_server.model.User;

/**
 * This class is responsible for mapping {@link RegisterDto} to {@link User} entity.
 * Uses MapStruct processor to generate {@link UserMapperImpl}.
 * <p>
 * The mapper is registered as a Spring Component, so it can be injected into services or other components that require user mapping functionality.
 */
@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public abstract class UserMapper {

    @Mapping(target = "updated", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "exams", ignore = true)
    @Mapping(target = "created", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "enabled", constant = "true")
    public abstract User fromDto(final RegisterDto registerDto);

    public abstract UserDto toDto(final User user);

}
