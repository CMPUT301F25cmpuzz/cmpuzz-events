package com.example.cmpuzz_events.ui.admin;

import com.google.firebase.storage.StorageReference;

public class ImageItem {
    private String name;
    private String url;
    private StorageReference reference;
    
    public ImageItem() {
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUrl() {
        return url;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public StorageReference getReference() {
        return reference;
    }
    
    public void setReference(StorageReference reference) {
        this.reference = reference;
    }
}
