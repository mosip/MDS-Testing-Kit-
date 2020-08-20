package io.mosip.mds.service;

import io.mosip.mds.dto.ComposeRequestDto;
import io.mosip.mds.dto.DeviceInfoResponse;
import io.mosip.mds.dto.DiscoverResponse;
import io.mosip.mds.dto.TestRun;
import io.mosip.mds.dto.ValidateResponseRequestDto;
import io.mosip.mds.dto.postresponse.ComposeRequestResponseDto;

public interface TestRunnerService {
	
	//public ComposeRequestResponseDto composeRequest(@RequestBody ComposeRequestDto composeRequestDto);
	
	//public ValidateResponseDto validateResponse(@RequestBody ValidateResponseRequestDto validateRequestDto);

	public TestRun validateResponse(ValidateResponseRequestDto validateRequestDto);

	public ComposeRequestResponseDto composeRequest(ComposeRequestDto composeRequestDto);

	public TestRun composeRequestForAllTests(ComposeRequestDto composeRequestDto);

	public DiscoverResponse[] decodeDiscoverInfo(String discoverInfo);

	public DeviceInfoResponse[] decodeDeviceInfo(String deviceInfo);
}
