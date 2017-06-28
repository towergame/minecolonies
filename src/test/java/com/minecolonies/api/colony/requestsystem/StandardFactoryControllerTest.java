package com.minecolonies.api.colony.requestsystem;

import com.google.common.reflect.TypeToken;
import com.minecolonies.api.colony.requestsystem.factory.IFactory;
import com.minecolonies.api.colony.requestsystem.factory.IFactoryController;
import com.minecolonies.api.colony.requestsystem.token.IToken;
import com.minecolonies.api.colony.requestsystem.token.StandardToken;
import com.minecolonies.api.colony.requestsystem.token.StandardTokenFactory;
import com.minecolonies.coremod.colony.requestsystem.locations.EntityLocation;
import com.minecolonies.coremod.colony.requestsystem.locations.StaticLocation;
import com.minecolonies.coremod.colony.requestsystem.requests.StandardRequestFactories;
import net.minecraft.nbt.NBTTagCompound;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Test for the implementation of {@link IFactoryController} in {@link StandardFactoryController}
 * Uses the {@link StandardTokenFactory} to Test the {@link StandardFactoryController}
 */
public class StandardFactoryControllerTest
{
    private StandardTokenFactory factory;

    @Before
    public void setUp() throws Exception
    {
        factory = new StandardTokenFactory();
        StandardFactoryController.getInstance().registerNewFactory(factory);
    }

    @After
    public void tearDown() throws Exception
    {
        StandardFactoryController.reset();
        factory = null;
    }

    @Test
    public void getFactoryForInput() throws Exception
    {
        IFactory<UUID, ?> inputBasedFactory = StandardFactoryController.getInstance().getFactoryForInput(new TypeToken<UUID>() {});
        assertEquals(inputBasedFactory, factory);
    }

    @Test
    public void getFactoryForOutput() throws Exception
    {
        IFactory<?, StandardToken> outputBasedFactory = StandardFactoryController.getInstance().getFactoryForOutput(new TypeToken<StandardToken>() {});
        assertEquals(outputBasedFactory, factory);
    }

    @Test
    public void registerNewFactory() throws Exception
    {
        StandardFactoryController.getInstance().registerNewFactory(new StaticLocation.Factory());
        StandardFactoryController.getInstance().registerNewFactory(new EntityLocation.Factory());
        StandardFactoryController.getInstance().registerNewFactory(new StandardRequestFactories.ItemStackFactory());
        StandardFactoryController.getInstance().registerNewFactory(new StandardRequestFactories.DeliveryFactory());
    }

    @Test(expected = IllegalArgumentException.class)
    public void registerNewFactoryDuplicate() throws Exception
    {
        StandardFactoryController.getInstance().registerNewFactory(factory);
        assertFalse(true);
    }

    @Test
    public void serialize() throws Exception
    {
        StandardToken standardToken = new StandardToken(UUID.randomUUID());
        IToken token = standardToken;

        NBTTagCompound compound = StandardFactoryController.getInstance().serialize(token);

        assertTrue(compound.hasKey(StandardFactoryController.NBT_TYPE));
        assertTrue(compound.hasKey(StandardFactoryController.NBT_DATA));
        assertEquals(compound.getString(StandardFactoryController.NBT_TYPE), new TypeToken<StandardToken>() {}.toString());
        assertEquals(compound.getCompoundTag(StandardFactoryController.NBT_DATA).getLong(StandardTokenFactory.NBT_MSB), standardToken.getIdentifier().getMostSignificantBits());
        assertEquals(compound.getCompoundTag(StandardFactoryController.NBT_DATA).getLong(StandardTokenFactory.NBT_LSB), standardToken.getIdentifier().getLeastSignificantBits());
    }

    @Test
    public void deserialize() throws Exception
    {
        StandardToken standardToken = new StandardToken(UUID.randomUUID());
        IToken token = standardToken;

        NBTTagCompound compound = StandardFactoryController.getInstance().serialize(token);
        IToken deserialize = StandardFactoryController.getInstance().deserialize(compound);

        assertEquals(token, deserialize);
    }

    @Test
    public void getNewInstance() throws Exception
    {
        UUID id = UUID.randomUUID();
        IToken token = new StandardToken(id);

        IToken output = StandardFactoryController.getInstance().getNewInstance(id, new TypeToken<IToken<UUID>>() {});

        assertEquals(output, token);
    }
}