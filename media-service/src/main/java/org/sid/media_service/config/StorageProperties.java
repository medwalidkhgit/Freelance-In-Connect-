package org.sid.media_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "azure.storage")
public class StorageProperties {

    private String connectionString;
    private String profilePicturesContainer = "profile-pictures";
    private String companyLogosContainer = "company-logos";
    private String cvsContainer = "cvs";
    private long imageMaxBytes = 5 * 1024 * 1024;
    private long cvMaxBytes = 10 * 1024 * 1024;
    private long readSasTtlMinutes = 60;

    public String getConnectionString() {
        return connectionString;
    }

    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }

    public String getProfilePicturesContainer() {
        return profilePicturesContainer;
    }

    public void setProfilePicturesContainer(String profilePicturesContainer) {
        this.profilePicturesContainer = profilePicturesContainer;
    }

    public String getCompanyLogosContainer() {
        return companyLogosContainer;
    }

    public void setCompanyLogosContainer(String companyLogosContainer) {
        this.companyLogosContainer = companyLogosContainer;
    }

    public String getCvsContainer() {
        return cvsContainer;
    }

    public void setCvsContainer(String cvsContainer) {
        this.cvsContainer = cvsContainer;
    }

    public long getImageMaxBytes() {
        return imageMaxBytes;
    }

    public void setImageMaxBytes(long imageMaxBytes) {
        this.imageMaxBytes = imageMaxBytes;
    }

    public long getCvMaxBytes() {
        return cvMaxBytes;
    }

    public void setCvMaxBytes(long cvMaxBytes) {
        this.cvMaxBytes = cvMaxBytes;
    }

    public long getReadSasTtlMinutes() {
        return readSasTtlMinutes;
    }

    public void setReadSasTtlMinutes(long readSasTtlMinutes) {
        this.readSasTtlMinutes = readSasTtlMinutes;
    }
}
