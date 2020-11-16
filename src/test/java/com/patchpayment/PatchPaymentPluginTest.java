package com.patchpayment;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class PatchPaymentPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(PatchPaymentPlugin.class);
		RuneLite.main(args);
	}
}