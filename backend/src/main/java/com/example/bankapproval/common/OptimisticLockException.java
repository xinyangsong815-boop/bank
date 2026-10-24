package com.example.bankapproval.common;

public class OptimisticLockException extends BizException {
    public OptimisticLockException() {
        super("CONCURRENT_MODIFICATION", "数据已被其他用户修改，请刷新后重试");
    }
}
