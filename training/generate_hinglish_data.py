import random, csv
random.seed(7)
banks=["SBI","HDFC Bank","ICICI Bank","Axis Bank","PNB","Kotak Bank","Bank of Baroda","Canara Bank"]
merch=["Amazon","Flipkart","Swiggy","Zomato","BigBasket","Myntra","IRCTC","Uber","Ola","Jio Mart","Blinkit","PhonePe"]
names=["Rahul","Priya","Amit","Sneha","Vikram","Neha","Rohan","Anita","Karan","Pooja"]
tel=["Airtel","Jio","Vi","BSNL"]
def amt(): return random.choice([49,99,149,199,239,299,349,499,599,749,999,1200,1500,2300,4500,7800,12500])
def acct(): return "XX"+str(random.randint(1000,9999))
def dt(): return f"{random.randint(1,28)}-{random.choice(['Oct','Nov','Dec','Sep'])}"
def pnr(): return str(random.randint(1000000000,9999999999))
def otp(): return str(random.randint(100000,999999))
legit_t=[
 lambda: f"Rs {amt()} debited from A/c {acct()} on {dt()} at {random.choice(merch)}. -{random.choice(banks)}",
 lambda: f"Rs {amt()} credited to your A/c {acct()} via UPI from {random.choice(names)}. Avl bal Rs {amt()*3}",
 lambda: f"{otp()} is your OTP for transaction of Rs {amt()}. Do not share it with anyone. -{random.choice(banks)}",
 lambda: f"Use OTP {otp()} to login to {random.choice(merch)}. Valid for 10 minutes. Never share OTP.",
 lambda: f"{random.choice(tel)}: Recharge of Rs {amt()} successful. Validity {random.choice([28,56,84])} days. Thank you",
 lambda: f"{random.choice(tel)}: Your data pack is 80% used. Remaining {random.choice([0.5,1,2])} GB valid till {dt()}",
 lambda: f"Your {random.choice(merch)} order #{random.randint(10000,99999)} is out for delivery and will arrive today.",
 lambda: f"Your package from {random.choice(merch)} has been delivered. Thanks for shopping with us.",
 lambda: f"IRCTC: PNR {pnr()} confirmed. Train {random.randint(10000,22999)} departs {dt()}. Happy journey",
 lambda: f"Dear customer, your {random.choice(banks)} credit card statement of Rs {amt()} is due on {dt()}. Pay via the official app.",
 lambda: f"Aapke account {acct()} se Rs {amt()} debit hue hain {dt()} ko. -{random.choice(banks)}",
 lambda: f"Aapke account mein Rs {amt()} UPI se credit hue hain. {random.choice(names)} ne bheje.",
 lambda: f"Aapka OTP {otp()} hai. Ise kisi ke saath share na karein. -{random.choice(banks)}",
 lambda: f"{random.choice(names)}, kal shaam {random.randint(4,8)} baje milte hain? Ghar aa jana.",
 lambda: f"Bhai paise bhej diye UPI se, check kar lena. Rs {amt()}",
 lambda: f"Your electricity bill of Rs {amt()} for {random.choice(['Oct','Nov'])} is generated. Due date {dt()}.",
 lambda: f"Reminder: Your appointment is scheduled for {dt()} at {random.randint(9,5)+0 if False else random.randint(10,17)}:00. Reply STOP to opt out.",
 lambda: f"Your {random.choice(merch)} refund of Rs {amt()} has been processed to your original payment method.",
 lambda: f"Ola: Your driver {random.choice(names)} is arriving in {random.randint(2,9)} mins. OTP for ride {random.randint(1000,9999)}",
 lambda: f"Ma, main {random.randint(7,10)} baje tak pahunch jaunga. Khana mat banana.",
]
scam_t=[
 lambda: f"Dear customer your {random.choice(banks)} KYC has expired. Update now at http://bit.ly/{random.choice(['kyc','upd','bnk'])}{random.randint(10,99)} or account will be blocked",
 lambda: f"Aapka {random.choice(banks)} account kal block ho jayega. Abhi KYC update karein: http://{random.choice(['sbi','hdfc','icici'])}-kyc{random.randint(1,99)}.{random.choice(['xyz','top','in.net'])}",
 lambda: f"Aapke account se Rs {amt()*10} ka unauthorized transaction hua. Turant is link par click karein: http://secure-{random.randint(100,999)}.xyz",
 lambda: f"Congratulations! You have won Rs {random.choice([5,10,25,50])} lakh in KBC lottery. Call {random.randint(7000000000,9999999999)} to claim now",
 lambda: f"Work from home job! Earn Rs {random.choice([3000,5000,8000])} daily. Just like YouTube videos. WhatsApp {random.randint(7000000000,9999999999)}",
 lambda: f"Aapko ghar baithe job mili hai. Rs {random.choice([3000,5000,8000])} roz kamayein. Abhi WhatsApp karein {random.randint(7000000000,9999999999)}",
 lambda: f"Your parcel is held at customs. Pay Rs {random.choice([49,89,99,149])} fee at http://post-{random.randint(10,99)}.{random.choice(['top','xyz','cc'])} within 24 hours",
 lambda: f"Aapke paise galti se aapke account mein aa gaye hain. Refund karne ke liye link par click karein: http://refund-{random.randint(10,99)}.xyz",
 lambda: f"Your electricity will be disconnected tonight 9:30 PM due to unpaid bill. Call {random.randint(7000000000,9999999999)} immediately",
 lambda: f"Bijli bill pending hai, aaj raat connection kat jayega. Turant is number par call karein {random.randint(7000000000,9999999999)}",
 lambda: f"Your {random.choice(banks)} debit card is blocked. Share OTP and card PIN to unblock. Call {random.randint(7000000000,9999999999)}",
 lambda: f"Aapka PAN card update nahi hai, account freeze hoga. Abhi update karein http://pan-update{random.randint(1,99)}.top",
 lambda: f"Dear user your SIM will be deactivated today. Complete KYC now: http://bit.ly/sim{random.randint(10,99)}",
 lambda: f"Income tax refund of Rs {amt()*8} approved. Claim now at http://itax-refund{random.randint(1,99)}.xyz",
 lambda: f"Hi, I am from Amazon HR. Earn Rs {random.choice([4000,6000])}/day part time. Join Telegram group http://t.me/job{random.randint(100,999)}",
 lambda: f"Urgent: Your {random.choice(['Netflix','Amazon Prime','Jio'])} subscription failed. Update payment now http://pay-{random.randint(10,99)}.cc to avoid suspension",
 lambda: f"Loan approved Rs {random.choice([50000,100000,200000])} instantly. No documents needed. Click http://loan{random.randint(1,99)}.xyz",
 lambda: f"Aapka UPI PIN expire ho gaya hai. Naya PIN set karne ke liye abhi click karein http://upi-pin{random.randint(1,99)}.top",
]
def build(tmpls,n):
    s=set()
    while len(s)<n:
        s.add(random.choice(tmpls)())
    return list(s)
rows=[(t,"normal") for t in build(legit_t,320)]+[(t,"scam") for t in build(scam_t,160)]
random.shuffle(rows)
with open("/tmp/out/my_hinglish.csv","w",newline="",encoding="utf-8") as f:
    w=csv.writer(f); w.writerow(["text","label"]); w.writerows(rows)
test=[
("Dear Sir, your account will be suspended. Verify KYC now using link: tinyurl.com/k9x2","scam"),
("Aapko Rs 2 lakh ka inaam laga hai. Claim karne ke liye processing fee Rs 500 bhejein","scam"),
("Hello, I'm Priya from recruitment. Easy task job, earn 800 per task. Message on Telegram","scam"),
("Final notice: your FASTag is blocked. Recharge immediately at http://fastag-help.top","scam"),
("Aapka Aadhaar link nahi hai, SIM band hogi. Abhi link karein: goo.gl/xy12","scam"),
("You have a pending courier. Update address within 12 hrs: http://dtdc-redeliver.xyz","scam"),
("Sir aapka gas subsidy pending hai, apna bank OTP bata dijiye tabhi paise aayenge","scam"),
("Your Paytm wallet is limited. Complete KYC today else balance will be frozen. Click here","scam"),
("Hum aapke bete ke accident ke liye Rs 15000 chahiye turant, is UPI id par bhejo","scam"),
("Exclusive: Invest Rs 5000 get Rs 50000 in 7 days. Guaranteed returns. WhatsApp now","scam"),
("Your Jio number won a free iPhone 15. Click to claim before offer ends tonight","scam"),
("Police notice: case registered on your Aadhaar. Call immediately to avoid arrest","scam"),
("Aapka electricity meter ka KYC adhura hai, 2 ghante mein connection kat jayega, call karo","scam"),
("Dear customer, your card ending 4412 is locked. Share the 6 digit code sent to you to unlock","scam"),
("Free recharge of Rs 599 for all Jio users! Click link to activate now bit.ly/jio599","scam"),
("Bank of India: Your account is under review. Login at http://boi-secure.cc to avoid closure","scam"),
("Congrats, you are selected for a government scheme. Pay Rs 200 registration to receive Rs 10000","scam"),
("Ji aapki UPI ID se galat transaction hua hai, refund ke liye AnyDesk download karein","scam"),
("Your DP World parcel needs a Rs 75 customs payment. Pay: http://cust-pay.top/75","scam"),
("Last chance! Win 1 crore. Send your bank account number and OTP to claim","scam"),
("Your Swiggy order has been picked up and will reach you in 25 minutes.","normal"),
("HDFC Bank: Rs 3,450.00 debited from a/c XX8821 for UPI txn to Reliance Retail on 20-Nov","normal"),
("Rs 2000 received from Mohit via PhonePe. UPI Ref 4455667788","normal"),
("Your OTP for Flipkart login is 558201. It expires in 5 minutes. Do not disclose it.","normal"),
("Jio: Your plan of Rs 299 has been renewed. Enjoy 1.5GB/day for 28 days.","normal"),
("Indian Railways: Your train 12302 is running 20 mins late. Platform 9.","normal"),
("Aapke SBI card ka bill Rs 5,600 hai, last date 28 Nov. Official app se pay karein.","normal"),
("Bhaiya kal ka meeting 11 baje shift ho gaya hai, office aana.","normal"),
("Your Uber ride receipt: Rs 245 paid via UPI. Thanks for riding.","normal"),
("Dr. Sharma's clinic: your appointment is confirmed for tomorrow 5 PM.","normal"),
("Amazon: Your refund of Rs 799 has been credited to your bank account.","normal"),
("Airtel Thanks: Your postpaid bill of Rs 499 is generated. Pay by 5 Dec in the Airtel app.","normal"),
("Mummy ne bola ki aaj raat khana bahar khayenge, ready rehna.","normal"),
("Your Aadhaar OTP is 774412. Valid for 10 min. Do not share with anyone. -UIDAI","normal"),
("Reminder: electricity bill Rs 1,150 due on 30-Nov. Pay on the official provider portal.","normal"),
("Bank alert: Rs 15000 credited to A/c XX2210 (salary). Avl Bal Rs 41,300. -Axis Bank","normal"),
("Your Myntra order has shipped. Track it in the app. Expected delivery 25 Nov.","normal"),
("Yaar notes bhej dena, kal exam hai. Thanks!","normal"),
("BSNL: Rs 199 recharge successful, validity 30 days. Balance updated.","normal"),
("Your cab is arriving. Driver Ramesh, OTP 4821, vehicle DL1C 4567.","normal"),
]
with open("/tmp/out/test40.csv","w",newline="",encoding="utf-8") as f:
    w=csv.writer(f); w.writerow(["text","label"]); w.writerows(test)
tr=set(r[0] for r in rows); print(len(rows), len(test), "overlap", len(tr & set(t for t,_ in test)))
