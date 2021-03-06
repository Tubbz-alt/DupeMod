package de.scribble.lp.dupemod;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class RefillingDupe {
	private Minecraft mc= Minecraft.getMinecraft(); 
	private TileEntityChest foundchest;
	private int chestcounter=0;
	private int chestitemcounter=0;
	private int itemcounter=0;
	
	public void killItems(List<EntityItem> list){
		for(int index=0;index<list.size();index++){
			list.get(index).setDead();
		}
	}
	
	/**
	 * Loads/refills chests and item entitys on the ground, essentially replacing them or spawning new Items
	 * @param file
	 * @param player
	 */
	public void refill(File file, EntityPlayer player){
		String[] coords;
		String[] items;
		String[] enchantments;
		World world = player.getEntityWorld();
		BlockPos playerPos = new BlockPos(player);
		try{
			BufferedReader Buff = new BufferedReader(new FileReader(file));
			String s;
			while (true){
				if((s=Buff.readLine()).equalsIgnoreCase("END")){
					break;
				}
				else if(s.startsWith("#")){				//comments
					continue;
				}
				else if(s.startsWith("Chest:")){		//refill chests
					while (true){
						if((s=Buff.readLine()).equalsIgnoreCase("\t-")){	//check for the end of the chest section
							break;
						}
						else if(s.startsWith("#")){		//comments
							continue;
						}
						else if(s.startsWith("\tx")){
							coords=s.split("(x=)|(,\\ y=)|(,\\ z=)");		//getting the coordinates of the chest
							if (world.getBlockState(new BlockPos(Integer.parseInt(coords[1]),Integer.parseInt(coords[2]),Integer.parseInt(coords[3]))).getBlock()== Blocks.CHEST||world.getBlockState(new BlockPos(Integer.parseInt(coords[1]),Integer.parseInt(coords[2]),Integer.parseInt(coords[3]))).getBlock()== Blocks.TRAPPED_CHEST){	//check if the targeted block is a chest or a redstone chest
									
								foundchest= (TileEntityChest) world.getTileEntity(new BlockPos(Integer.parseInt(coords[1]),Integer.parseInt(coords[2]),Integer.parseInt(coords[3])));
								
								/*Check if the player is too far away from the chest and prevents it from being refilled... A failsafe and cheat prevention*/
								if(playerPos.distanceSq((double)foundchest.getPos().getX(), (double)foundchest.getPos().getY(), (double)foundchest.getPos().getZ())>50.0){
										DupeMod.logger.error("Chest at "+Integer.parseInt(coords[1])+" "+Integer.parseInt(coords[2])+" "+Integer.parseInt(coords[3])+" is too far away! Distance: "+playerPos.distanceSq((double)foundchest.getPos().getX(), (double)foundchest.getPos().getY(), (double)foundchest.getPos().getZ()));
										continue;
								}
								while(true){
									if((s=Buff.readLine()).equalsIgnoreCase("\t\t-")){ 	//check for the end of the chest contains section
										break;
									}
									else if(s.startsWith("#")){		//comments
										continue;
									}
									else if(s.startsWith("\t\tSlot")){ 		//read out the items from the line and puts it into the chest slot
										items=s.split(";");
										
										/*items[2]= itemID, items[4]=amount, items[5]=damage*/
										ItemStack properties= new ItemStack(Item.getItemById(Integer.parseInt(items[2])),
																								Integer.parseInt(items[4]),
																								Integer.parseInt(items[5]));
										/*
										if(!items[7].equals("[]")){
											enchantments=items[7].split("(\\[\\{lvl:)|(s,id:)|(s\\},\\{lvl:)|(s\\})");
											for(int index=1;index<=(enchantments.length-2)/2;index++){
												properties.addEnchantment(Enchantment.getEnchantmentByID(Integer.parseInt(enchantments[2*index])), Integer.parseInt(enchantments[2*index-1]));
											}
										}
										if(!items[6].equals("null")){
											properties.setStackDisplayName(items[6]);
										}*/
										
										/*Adding NBT to the item*/
										NBTTagCompound newnbttag= new NBTTagCompound();
										try {
											newnbttag = JsonToNBT.getTagFromJson(items[6]);
										} catch (NBTException e) {
											DupeMod.logger.error("Something happened while trying to convert String to NBT");
											DupeMod.logger.catching(e);
										}
										properties.stackTagCompound=newnbttag;
										
										foundchest.setInventorySlotContents(Integer.parseInt(items[1]), properties);	//Set the item into the slot
										chestitemcounter++; //for logging
									}
								}chestcounter++; //for logging
							}
							else{	//Message if there is no chest at the specified coordinates, can happen when using /dupe
								DupeMod.logger.error("Didn't find a chest at "+Integer.parseInt(coords[1])+" "+Integer.parseInt(coords[2])+" "+Integer.parseInt(coords[3])+".");
								continue;
							}
						}
					}
				}
				else if(s.startsWith("Items:")){ 	//refill items on the ground, here just titled as "Items
					
					
					String[] position=s.split(":");
					BlockPos dupePos= new BlockPos(Integer.parseInt(position[1]),Integer.parseInt(position[2]),Integer.parseInt(position[3]));	//get the position where the s+q was done
					List<EntityItem> entitylist= world.getEntitiesWithinAABB(EntityItem.class, new AxisAlignedBB(dupePos).grow(10.0));			//get all entityitems around the player
					
					
					if(playerPos.distanceSq((double)dupePos.getX(),(double)dupePos.getY(),(double)dupePos.getZ())>=50.0){						//abort if the player is too far away from the duping position, cheat prevention and failsafe when using /dupe
						DupeMod.logger.error("Player moved too far from initial duping position. Aborting EntityDupe! DupePosition: ("+dupePos.getX()+";"+dupePos.getY()+";"+dupePos.getZ()+") Distance: "+playerPos.distanceSq((double)dupePos.getX(),(double)dupePos.getY(),(double)dupePos.getZ()));
						continue;
					}
					if(!entitylist.isEmpty()){	//Kill all items in the surrounding area
						killItems(entitylist);
					}
					while (true){
						if((s=Buff.readLine()).equalsIgnoreCase("\t-")){	//check for the end of the item section
							break;
						}
						else if(s.startsWith("#")){		//comments
							continue;
						}
						else if(s.startsWith("\tItem;")){
							String[] props=s.split(";");
							ItemStack Overflow= new ItemStack(Item.getItemById(Integer.parseInt(props[5])), //Create the ItemStack
									Integer.parseInt(props[7]),
									Integer.parseInt(props[8]));
							/*
							if(!props[10].equals("[]")){	//add Enchantments
								enchantments=props[10].split("(\\[\\{lvl:)|(s,id:)|(s\\},\\{lvl:)|(s\\})");
								for(int index=1;index<=(enchantments.length-2)/2;index++){
									Overflow.addEnchantment(Enchantment.getEnchantmentByID(Integer.parseInt(enchantments[2*index])), Integer.parseInt(enchantments[2*index-1]));
								}
							}
							if(!props[9].equals("null")){ //set customName
								Overflow.setStackDisplayName(props[9]);
							}*/
							//Adding NBT to the item
							NBTTagCompound newnbttag= new NBTTagCompound();
							try {
								newnbttag = JsonToNBT.getTagFromJson(props[11]);
							} catch (NBTException e) {
								DupeMod.logger.error("Something happened while trying to convert String to NBT");
								DupeMod.logger.catching(e);
							}
							Overflow.stackTagCompound=newnbttag;
							
							EntityItem newitem=new EntityItem(world, Double.parseDouble(props[2]), Double.parseDouble(props[3]), Double.parseDouble(props[4]), Overflow);
							world.spawnEntity(newitem);
							
							//Apply the age
							newitem.age=Integer.parseInt(props[9]);
							
							//Apply the pickupdelay
							newitem.pickupDelay=Integer.parseInt(props[10]);
							
							
							newitem.motionX=0;	//set the motion to zero so it doesn't fly around
							newitem.motionY=0;
							newitem.motionZ=0;
							itemcounter++; //for logging
						}
					}
				}
			}
			Buff.close();
			if(chestcounter==0&&itemcounter==0){
				DupeMod.logger.info("Nothing refilled");
			}else{
				DupeMod.logger.info("Refilled "+chestcounter+" chest(s) with "+chestitemcounter+" item(s) and spawned "+ itemcounter+ " item(s) on the ground.");
			}
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
}
