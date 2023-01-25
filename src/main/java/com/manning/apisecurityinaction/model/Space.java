package com.manning.apisecurityinaction.model;

import org.json.JSONObject;

public class Space {
    private final long spaceId;
    private final String owner;
    private final String name;

    public Space(final long spaceId, final String owner, final String name) {
        this.spaceId = spaceId;
        this.owner = owner;
        this.name = name;
    }

    @Override
    public String toString() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("spaceId", this.spaceId);
        jsonObject.put("name", this.name);
        jsonObject.put("owner", this.owner);

        return jsonObject.toString();
    }
}
