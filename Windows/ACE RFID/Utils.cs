﻿using System;
using System.Text;

namespace ACE_RFID
{
    internal class Utils
    {

        public static int GetMaterialLength(String materialWeight)
        {
            switch (materialWeight)
            {
                case "1 KG":
                    return 330;
                case "750 G":
                    return 247;
                case "600 G":
                    return 198;
                case "500 G":
                    return 165;
                case "250 G":
                    return 82;
            }
            return 330;
        }

        public static String GetMaterialWeight(int materialLength)
        {
            switch (materialLength)
            {
                case 330:
                    return "1 KG";
                case 247:
                    return "750 G";
                case 198:
                    return "600 G";
                case 165:
                    return "500 G";
                case 82:
                    return "250 G";
            }
            return "1 KG";
        }

        public static byte[] GetSku(string materialName)
        {
            string sku = "";
            switch (materialName)
            {
                case "ABS":
                    sku = "SHABBK-102";
                    break;
                case "PLA High Speed":
                    sku = "AHHSBK-103";
                    break;
                case "PLA Matte":
                    sku = "HYGBK-102";
                    break;
                case "PLA Silk":
                    sku = "AHSCWH-102";
                    break;
                case "TPU":
                    sku = "STPBK-101";
                    break;
                case "PLA":
                    sku = "AHPLBK-101";
                    break;
                case "PLA+":
                    sku = "AHPLPBK-102";
                    break;
                default:
                    sku = "";
                    break;
            }
            return System.Text.Encoding.ASCII.GetBytes(sku);
        }

        public static byte[] GetBrand(string materialName)
        {
            string brand = "";
            if (materialName == "ABS" || materialName == "PLA High Speed" ||
                materialName == "PLA Matte" || materialName == "PLA Silk" || materialName == "TPU" ||
                materialName == "PLA" || materialName == "PLA+")
            {
                brand = "AC";
            }
            return System.Text.Encoding.ASCII.GetBytes(brand);
        }

        public static int[] GetTemps(string materialName)
        {
            switch (materialName)
            {
                case "ABS":
                    return new int[] { 220, 250, 90, 100 };
                case "ASA":
                    return new int[] { 240, 280, 90, 100 };
                case "PLA":
                case "PLA High Speed":
                case "PLA Glow":
                    return new int[] { 190, 230, 50, 60 };
                case "PLA Marble":
                    return new int[] { 200, 230, 50, 60 };
                case "PLA Matte":
                case "PLA SE":
                    return new int[] { 190, 230, 55, 65 };
                case "PLA Silk":
                    return new int[] { 200, 230, 55, 65 };
                case "PETG":
                    return new int[] { 230, 250, 70, 90 };
                case "TPU":
                    return new int[] { 210, 230, 25, 60 };
                case "PLA+":
                    return new int[] { 210, 230, 45, 60 };
            }
            return new int[] { 200, 210, 50, 60 };
        }

        public static int[] GetDefaultTemps(String materialType)
        {
            switch (materialType)
            {
                case "ABS":
                    return new int[] { 220, 250, 90, 100 };
                case "ASA":
                    return new int[] { 240, 280, 90, 100 };
                case "HIPS":
                    return new int[] { 230, 245, 80, 100 };
                case "PA":
                    return new int[] { 220, 250, 70, 90 };
                case "PA-CF":
                    return new int[] { 260, 280, 80, 100 };
                case "PC":
                    return new int[] { 260, 300, 100, 110 };
                case "PETG":
                    return new int[] { 230, 250, 70, 90 };
                case "PLA":
                    return new int[] { 190, 230, 50, 60 };
                case "PLA+":
                    return new int[] { 210, 230, 45, 60 };
                case "PLA-CF":
                    return new int[] { 210, 240, 45, 65 };
                case "PVA":
                    return new int[] { 215, 225, 45, 60 };
                case "PP":
                    return new int[] { 225, 245, 80, 105 };
                case "TPU":
                    return new int[] { 210, 230, 25, 60 };
            }
            return new int[] { 185, 300, 45, 110 };
        }

        public static String[] filamentVendors = {
            "3Dgenius",
            "3DJake",
            "3DXTECH",
            "3D BEST-Q",
            "3D Hero",
            "3D-Fuel",
            "Aceaddity",
            "AddNorth",
            "Amazon Basics",
            "AMOLEN",
            "Ankermake",
            "Anycubic",
            "Atomic",
            "AzureFilm",
            "BASF",
            "Bblife",
            "BCN3D",
            "Beyond Plastic",
            "California Filament",
            "Capricorn",
            "CC3D",
            "colorFabb",
            "Comgrow",
            "Cookiecad",
            "Creality",
            "CERPRiSE",
            "Das Filament",
            "DO3D",
            "DOW",
            "DSM",
            "Duramic",
            "ELEGOO",
            "Eryone",
            "Essentium",
            "eSUN",
            "Extrudr",
            "Fiberforce",
            "Fiberlogy",
            "FilaCube",
            "Filamentive",
            "Fillamentum",
            "FLASHFORGE",
            "Formfutura",
            "Francofil",
            "FilamentOne",
            "Fil X",
            "GEEETECH",
            "Giantarm",
            "Gizmo Dorks",
            "GreenGate3D",
            "HATCHBOX",
            "Hello3D",
            "IC3D",
            "IEMAI",
            "IIID Max",
            "INLAND",
            "iProspect",
            "iSANMATE",
            "Justmaker",
            "Keene Village Plastics",
            "Kexcelled",
            "LDO",
            "MakerBot",
            "MatterHackers",
            "MIKA3D",
            "NinjaTek",
            "Nobufil",
            "Novamaker",
            "OVERTURE",
            "OVVNYXE",
            "Polymaker",
            "Priline",
            "Printed Solid",
            "Protopasta",
            "Prusament",
            "Push Plastic",
            "R3D",
            "Re-pet3D",
            "Recreus",
            "Regen",
            "Sain SMART",
            "SliceWorx",
            "Snapmaker",
            "SnoLabs",
            "Spectrum",
            "SUNLU",
            "TTYT3D",
            "Tianse",
            "UltiMaker",
            "Valment",
            "Verbatim",
            "VO3D",
            "Voxelab",
            "VOXELPLA",
            "YOOPAI",
            "Yousu",
            "Ziro",
            "Zyltech"};

        public static String[] filamentTypes = {
            "ABS",
            "ASA",
            "HIPS",
            "PA",
            "PA-CF",
            "PC",
            "PETG",
            "PLA",
            "PLA-CF",
            "PVA",
            "PP",
            "TPU"};

        public static byte[] RevArray(byte[] array)
        {
            Array.Reverse(array);
            return array;
        }

        public static int ParseNumber(byte[] byteArray)
        {
            int result = 0;
            foreach (byte b in RevArray(byteArray))
            {
                result = (result << 8) | (b & 0xFF);
            }
            return result;
        }

        public static byte[] NumToBytes(int value)
        {
            return RevArray(new byte[] { (byte)(value >> 8), (byte)value });
        }

        public static byte[] ParseColor(string hexString)
        {
            int length = hexString.Length;
            byte[] byteArray = new byte[length / 2];
            for (int i = 0; i < length; i += 2)
            {
                try
                {
                    byteArray[i / 2] = (byte)Convert.ToByte(hexString.Substring(i, 2), 16);
                }
                catch (Exception)
                {
                    return new byte[] { 0xFF, 0xFF, 0x00, 0x00 };
                }
            }
            return RevArray(byteArray);
        }

        public static string ParseColor(byte[] byteArray)
        {
            try
            {
                StringBuilder hexString = new StringBuilder();
                foreach (byte b in RevArray(byteArray))
                {
                    hexString.Append(b.ToString("x2"));
                }
                return hexString.ToString();
            }
            catch (Exception)
            {
                return "0000FF";
            }
        }



        public static byte[] SubArray(byte[] source, int startIndex, int length)
        {
            if (source == null)
            {
                return null;
            }

            int sourceLength = source.Length;
            if (startIndex < 0 || startIndex >= sourceLength || length <= 0)
            {
                return Array.Empty<byte>();
            }

            int endIndex = Math.Min(startIndex + length, sourceLength);
            int actualLength = endIndex - startIndex;
            byte[] result = new byte[actualLength];
            Array.Copy(source, startIndex, result, 0, actualLength);
            return result;
        }


    }
}
