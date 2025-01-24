package com.app.upload.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_video_info")
public class TVideoInfo {
    @jakarta.persistence.Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;
    private String guid;
    @Column(length = 1000)
    private String filename;
    private Long size;
    private String extension;
    private String source_resolution;
    private Long t_mst_user_id;
    @Column(nullable = false, columnDefinition = "int default 1")
    private int is_active = 1;
    @Column(nullable = false, columnDefinition = "TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    private LocalDateTime trans_datetime;

    public TVideoInfo(String guid, String filename, Long size, String extension, String source_resolution, Long t_mst_user_id) {
        this.guid = guid;
        this.filename = filename;
        this.size = size;
        this.extension = extension;
        this.source_resolution = source_resolution;
        this.t_mst_user_id = t_mst_user_id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public String getSource_resolution() {
        return source_resolution;
    }

    public void setSource_resolution(String source_resolution) {
        this.source_resolution = source_resolution;
    }

    public Long getT_mst_user_id() {
        return t_mst_user_id;
    }

    public void setT_mst_user_id(Long t_mst_user_id) {
        this.t_mst_user_id = t_mst_user_id;
    }

    public int getIs_active() {
        return is_active;
    }

    public void setIs_active(int is_active) {
        this.is_active = is_active;
    }

    public LocalDateTime getTrans_datetime() {
        return trans_datetime;
    }

    public void setTrans_datetime(LocalDateTime trans_datetime) {
        this.trans_datetime = trans_datetime;
    }
}
