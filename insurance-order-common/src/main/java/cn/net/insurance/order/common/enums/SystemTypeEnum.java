package cn.net.insurance.order.common.enums;


public enum SystemTypeEnum {

    INSURANCE(1,"oms");

    private String name;
    private Integer value;

    SystemTypeEnum(Integer value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
