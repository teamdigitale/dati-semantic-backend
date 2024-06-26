package it.gov.innovazione.ndc.alerter.data;

import it.gov.innovazione.ndc.alerter.dto.ProfileDto;
import it.gov.innovazione.ndc.alerter.entities.EventCategory;
import it.gov.innovazione.ndc.alerter.entities.Profile;
import lombok.SneakyThrows;
import org.mapstruct.Mapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public abstract class ProfileMapper implements EntityMapper<Profile, ProfileDto> {

    @SneakyThrows
    protected List<EventCategory> stringListToEventCategoryList(List<String> list) {
        if (list == null) {
            return new ArrayList<>();
        }

        List<EventCategory> list1 = new ArrayList<EventCategory>(list.size());
        for (String string : list) {
            list1.add(asEventCategoryEnum(string));
        }

        return list1;
    }

    private EventCategory asEventCategoryEnum(String string) {
        try {
            return EventCategory.valueOf(string);
        } catch (IllegalArgumentException e) {
            String validCategories = Arrays.stream(EventCategory.values()).sequential()
                    .map(Enum::name)
                    .collect(Collectors.joining(", "));
            throw new EntityService.ConflictingOperationException("Invalid event category: " + string + ". Valid categories are: " + validCategories);
        }
    }

}
