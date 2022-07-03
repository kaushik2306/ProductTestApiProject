package com.test.api.testingapi;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import com.company.drivers.product.ProductOperations;
import com.company.inputmodel.product.ProductModel;
import com.company.utils.OperationUtils;
import com.test.api.utils.InputDataUtils;

public class TestApi {
	
	/**
	 * Testing API for Login
	 * @param productUID - multiple uid can be passed with <q>#</q> separated value
	 * @param value
	 */
	@Test
	@Parameters({"productUID"})
	public void login(String productUID){
		SoftAssert sa = new SoftAssert();
		System.out.println("Product UID is "+productUID);
		for(ProductModel product : InputDataUtils.getByUid(productUID, ProductModel.class, sa)){
			try {
				ProductOperations operations = OperationUtils.getInstance(product.getUid(), ProductOperations.class);
				operations.login(product);				
			} catch (Exception e) {
				sa.fail("Failed to login on product with uid "+product.getUid(),e);
			}
		}
		sa.assertAll();
	}
	
	/**
	 * Testing API for Logout
	 * @param productUID - multiple uid can be passed with <q>#</q> separated value
	 * @param value
	 */
	@Test
	@Parameters({"productUID"})
	public void logout(String productUID){
		SoftAssert sa = new SoftAssert();
		System.out.println("Product UID is "+productUID);
		for(ProductModel product : InputDataUtils.getByUid(productUID, ProductModel.class, sa)){	
			try {
				ProductOperations operations = OperationUtils.getInstance(product.getUid(), ProductOperations.class);
				operations.logout(product);				
			} catch (Exception e) {
				sa.fail("Failed to logout on product with uid "+product.getUid(),e);
			}
		}
		sa.assertAll();
	}
}
