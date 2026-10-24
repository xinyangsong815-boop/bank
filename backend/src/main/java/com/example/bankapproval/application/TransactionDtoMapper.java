package com.example.bankapproval.application;

import com.example.bankapproval.api.dto.TransactionResponse;
import com.example.bankapproval.domain.model.TransactionApplication;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/** MapStruct 在编译期生成字段复制代码；敏感字段由 Service 解密后脱敏，避免无意明文映射。 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionDtoMapper {
    @Mapping(target = "mobileMasked", ignore = true)
    @Mapping(target = "idCardMasked", ignore = true)
    @Mapping(target = "accountMasked", ignore = true)
    @Mapping(target = "counterpartyAccountMasked", ignore = true)
    TransactionResponse toResponse(TransactionApplication transaction);
}
