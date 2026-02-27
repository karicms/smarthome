package com.cms.smart_home_agent.vo;

public class Result<T> {
    public int code;
    public String message;
    public T data;

    private Result(int code, String message, T data)
    {
        this.code=code;
        this.message=message;
        this.data=data;
    }
    public static <T>Result success()
    {
        Result r=new Result(200,"suc",null);

        return r;
    }
    public static <T>Result success(T data)
    {
        Result r=new Result(200,"suc",data);
        return r;
    }

    public static <T>Result fail(T data) {
        Result r=new Result(0,"fail",data);
        return r;
    }
}