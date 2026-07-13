package com.met.mto.service;

import com.met.mto.dto.AppVersionResponse;

public interface AppVersionService {

    AppVersionResponse latest(String platform, Integer currentVersionCode);
}
