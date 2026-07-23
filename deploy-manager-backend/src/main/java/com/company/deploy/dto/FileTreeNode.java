package com.company.deploy.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class FileTreeNode {

    private String id;
    private String name;
    private String type;
    private String path;
    private List<FileTreeNode> children = new ArrayList<>();

    public FileTreeNode() {}

    public FileTreeNode(String id, String name, String type, String path) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.path = path;
    }
}
