package leti_sisdis_6.happatients.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = false)
public class ContactDetailsUpdateDTO {
    @JsonProperty(required = false)
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
    private String phoneNumber;

    @JsonProperty(required = false)
    @Valid
    private AddressDTO address;

    @JsonProperty(required = false)
    @Valid
    private PhotoDTO photo;

    @Data
    public static class AddressDTO {
        private String street;
        private String city;
        private String postalCode;
        private String country;
    }

    @Data
    public static class PhotoDTO {
        private String url;
        private String uploadedAt;
    }
} 