package com.vladyka.lpnu.model.enums;

public enum StudyForm {

  FULL_TIME("full-time"),
  PART_TIME("part-time");

  private String friendlyName;

  StudyForm(String friendlyName) {
    this.friendlyName = friendlyName;
  }

  public String getFriendlyName() {
    return friendlyName;
  }
}
