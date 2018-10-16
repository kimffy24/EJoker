package com.jiefzz.ejoker_quasar_test;

import com.jiefzz.ejoker.z.common.context.annotation.context.EService;
import com.jiefzz.ejoker.z.common.rpc.IClientNodeIPAddressProvider;

@EService
public class ClientNodeIPAddressProvider4Test implements IClientNodeIPAddressProvider {

	@Override
	public String getClientNodeIPAddress() {
		return "127.0.0.1";
	}

}
