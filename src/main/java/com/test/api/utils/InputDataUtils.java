package com.test.api.utils;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.asserts.SoftAssert;

import com.company.inputmodel.InputModel;
import com.company.inputmodel.product.ProductModel;
import com.test.api.constants.IConstant;
import com.test.api.listeners.MySuiteListener;

public class InputDataUtils implements IConstant {
	
	static Logger logger = LoggerFactory.getLogger(InputDataUtils.class);
	
	public static synchronized <T> List<T> getByUid(String uid, Class<T> clazzType, SoftAssert sa) {
		List<T> valueToReturn = new ArrayList<>();
		InputModel model = MySuiteListener.getInputModelObj();
		if(clazzType.getName().equals(ProductModel.class.getName())){
			for(String currentUid : uid.split(VALUE_SEPRATOR)){
				T object = (T) model.getProductList().stream().filter(e -> e.getUid().equals(currentUid.trim())).findFirst().orElse(null);
				if(object!=null){					
					valueToReturn.add(object);
				}else{
					sa.fail("Failed to find object of type "+clazzType.getName()+" having UID "+currentUid.trim());
				}
			}
		}
		return valueToReturn;
	}
}
