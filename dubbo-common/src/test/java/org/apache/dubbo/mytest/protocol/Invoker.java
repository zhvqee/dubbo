package org.apache.dubbo.mytest.protocol;

public class Invoker {

    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "Invoker{" +
                "id=" + id +
                '}';
    }
}
