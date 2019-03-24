package com.xuecheng.manage_cms.service;

import com.xuecheng.framework.domain.cms.CmsConfig;
import com.xuecheng.manage_cms.dao.CmsConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class CmsConfigService {

    @Autowired
    private CmsConfigRepository cmsConfigRepository;

    public CmsConfig getModel(String id) {
        Optional<CmsConfig> cmsConfigOptional = cmsConfigRepository.findById(id);
        if (!cmsConfigOptional.isPresent()) {
            return null;
        }
        return cmsConfigOptional.get();
    }
}
