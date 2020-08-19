package io.mosip.mds.validator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.mosip.mds.dto.CaptureResponse;
import io.mosip.mds.dto.CaptureResponse.CaptureBiometric;
import io.mosip.mds.dto.CaptureResponse.CaptureBiometricData;
import io.mosip.mds.dto.ValidateResponseRequestDto;
import io.mosip.mds.dto.Validation;
import io.mosip.mds.entitiy.Validator;

@Component
public class ValidValueRCaptureResponseValidator extends Validator{

	private final List<String> bioSubTypeFingerList= getBioSubTypeFinger();
	private final List<String> bioSubTypeIrisList = getBioSubTypeIris();

	@Autowired
	private ObjectMapper jsonMapper;

	Validation validation = new Validation();

	@Autowired
	CommonValidator commonValidator;

	public ValidValueRCaptureResponseValidator() {
		super("ValidValueRCaptureResponseValidator", "Valid Value Registration Capture Response Validator");
	}

	@Override
	protected List<Validation> DoValidate(ValidateResponseRequestDto response) throws JsonProcessingException {
		List<Validation> validations = new ArrayList<>();
		validation = commonValidator.setFieldExpected("response","Expected whole Jsone Response",jsonMapper.writeValueAsString(response));		
		if(Objects.nonNull(response))
		{
			validations.add(validation);
			validation = commonValidator.setFieldExpected("mdsDecodedResponse","Expected whole RCapture decoded Jsone Response",jsonMapper.writeValueAsString(response.getMdsDecodedResponse()));
			// Check for Biometrics block
			validations.add(validation);

			CaptureResponse registrationCaptureResponse = (CaptureResponse) response.getMdsDecodedResponse();
			if(Objects.nonNull(registrationCaptureResponse))
			{
				validation = commonValidator.setFieldExpected("registrationCaptureResponse.biometrics","Expected Array of biometric data",jsonMapper.writeValueAsString(registrationCaptureResponse.biometrics));
				if(registrationCaptureResponse.biometrics == null || registrationCaptureResponse.biometrics.length == 0)
				{
					commonValidator.setFoundMessageStatus(validation,registrationCaptureResponse.biometrics.toString(),"RCapture response does not contain biometrics block",CommonConstant.FAILED);
				}
				validations.add(validation);
				for(CaptureBiometric bb:registrationCaptureResponse.biometrics)
				{
					CaptureBiometricData dataDecoded = bb.dataDecoded;
					if(Objects.nonNull(dataDecoded)) {
						validations=validateActualValueDatadecoded(validations, dataDecoded);
						//TODO check for env
						validation = commonValidator.setFieldExpected("dataDecoded.env","Staging | Developer | Pre-Production | Production",dataDecoded.env);
						if( !dataDecoded.env.equals(CommonConstant.STAGING) && !dataDecoded.env.equals(CommonConstant.DEVELOPER)
								&& !dataDecoded.env.equals(CommonConstant.PRE_PRODUCTION) && !dataDecoded.env.equals(CommonConstant.PRODUCTION))
						{
							commonValidator.setFoundMessageStatus(validation,dataDecoded.env,"Capture response biometrics-dataDecoded env is invalid",CommonConstant.FAILED);
						}
						validations.add(validation);

						//TODO check time stamp for ISO Format date time with timezone
						validations=commonValidator.validateTimeStamp(dataDecoded.timestamp,validations);

						//TODO check for requestedScore
						//TODO check for quality score
					}
				}
			}
			else
			{
				commonValidator.setFoundMessageStatus(validation,"Found RegistrationCapture Decoded is null","RegistrationCapture response is empty",CommonConstant.FAILED);
				validations.add(validation);
			}

		}
		else
		{
			commonValidator.setFoundMessageStatus(validation,"Expected response is null","Response is empty",CommonConstant.FAILED);
			validations.add(validation);
		}
		return validations;
	}
	private List<Validation> validateActualValueDatadecoded(List<Validation> validations, CaptureBiometricData dataDecoded) {
		// Check for bioType elements
		validation = commonValidator.setFieldExpected("dataDecoded.bioType","Finger | Iris| Face",dataDecoded.bioType);		
		if(!dataDecoded.bioType.equals(CommonConstant.FINGER) && !dataDecoded.bioType.equals(CommonConstant.IRIS) && !dataDecoded.bioType.equals(CommonConstant.FACE))
		{
			commonValidator.setFoundMessageStatus(validation,dataDecoded.bioType,"Registration Capture response biometrics-dataDecoded bioType is invalid",CommonConstant.FAILED);
			validations.add(validation);

		}
		else
		{
			validations.add(validation);
			//Check for bioSubType
			validations = validateBioSubType(validations, dataDecoded);
		}


		//TODO Check for digitalId dataDecoded.digitalId
		validations=validateDigitalId(dataDecoded, validations);

		//Check for purpose elements
		validation = commonValidator.setFieldExpected("dataDecoded.purpose"," Auth or Registration",dataDecoded.purpose);
		if(!dataDecoded.purpose.equals(CommonConstant.AUTH) && !dataDecoded.purpose.equals(CommonConstant.REGISTRATION) )
		{
			commonValidator.setFoundMessageStatus(validation,dataDecoded.purpose,"Registration Capture response biometrics-dataDecoded purpose is invalid",CommonConstant.FAILED);
		}
		validations.add(validation);
		return validations;
	}

	private List<Validation> validateBioSubType(List<Validation> validations, CaptureBiometricData dataDecoded) {
		switch(dataDecoded.bioType) {
		// Check for bioSubType of Finger elements
		case CommonConstant.FINGER:
			validation = commonValidator.setFieldExpected("dataDecoded.bioSubType","For Finger: [Left IndexFinger, Left MiddleFinger, "
					+ "Left RingFinger, Left LittleFinger, Left Thumb, Right IndexFinger,"
					+ " Right MiddleFinger, Right RingFinger, Right LittleFinger, Right Thumb, UNKNOWN] ",dataDecoded.bioSubType);		
			if(!bioSubTypeFingerList.contains(dataDecoded.bioSubType))
			{
				commonValidator.setFoundMessageStatus(validation,dataDecoded.bioSubType,"Registration Capture response bioSubType is invalid for Finger",CommonConstant.FAILED);
			}
			validations.add(validation);
			break;

		case CommonConstant.IRIS:
			// Check for bioSubType of Iris elements
			validation = commonValidator.setFieldExpected("dataDecoded.bioSubType","[Left, Right, UNKNOWN]",dataDecoded.bioSubType);
			if(!bioSubTypeIrisList.contains(dataDecoded.bioSubType))
			{
				commonValidator.setFoundMessageStatus(validation,dataDecoded.bioSubType,"Registration Capture response bioSubType is invalid for Iris",CommonConstant.FAILED);
			}
			validations.add(validation);
			break;
		case CommonConstant.FACE:	
			// Check for bioSubType of Face elements
			validation = commonValidator.setFieldExpected("dataDecoded.bioSubType","No bioSubType",dataDecoded.bioSubType);
			if(!(dataDecoded.bioSubType == null || dataDecoded.bioSubType.isEmpty()))
			{
				commonValidator.setFoundMessageStatus(validation,dataDecoded.bioSubType,"Registration Capture response bioSubType for Face should be empty",CommonConstant.FAILED);
			}
			validations.add(validation);
		}
		return validations;
	}

	private List<Validation> validateDigitalId(CaptureBiometricData dataDecoded,List<Validation> validations) {
		validations = commonValidator.validateDecodedSignedDigitalID(dataDecoded.digitalId,validations);
		return validations;
	}

	public List<String> getBioSubTypeIris() {
		List<String> bioSubTypeIrisList = new ArrayList<String>();
		bioSubTypeIrisList.add(CommonConstant.LEFT);
		bioSubTypeIrisList.add(CommonConstant.RIGHT);
		bioSubTypeIrisList.add( CommonConstant.UNKNOWN);
		return bioSubTypeIrisList;
	}
	public List<String> getBioSubTypeFinger() {
		List<String> bioSubTypeFingerList=new ArrayList<String>();
		bioSubTypeFingerList.add(CommonConstant.LEFT_INDEX_FINGER);
		bioSubTypeFingerList.add(CommonConstant.LEFT_MIDDLE_FINGER);
		bioSubTypeFingerList.add(CommonConstant.LEFT_RING_FINGER);
		bioSubTypeFingerList.add(CommonConstant.LEFT_LITTLE_FINGER);
		bioSubTypeFingerList.add(CommonConstant.LEFT_THUMB);
		bioSubTypeFingerList.add(CommonConstant.RIGHT_INDEX_FINGER);
		bioSubTypeFingerList.add(CommonConstant.RIGHT_MIDDLE_FINGER);
		bioSubTypeFingerList.add(CommonConstant.RIGHT_RING_FINGER);
		bioSubTypeFingerList.add(CommonConstant.RIGHT_LITTLE_FINGER);
		bioSubTypeFingerList.add(CommonConstant.RIGHT_THUMB);
		bioSubTypeFingerList.add(CommonConstant.UNKNOWN);

		return bioSubTypeFingerList;
	}

	@Override
	protected boolean checkVersionSupport(String version) {
		//TODO
		if(version.equals("0.9.5"))
			return true;

		return false;
	}
	@Override
	protected String supportedVersion() {
		// TODO return type of mds spec version supported
		return "0.9.5";
	}
}
