<wizard-report>
# Amplitude post-wizard report

_This report was generated automatically by the Amplitude wizard. The agent didn't produce one this run, so the wizard wrote a minimal recap from what it knows._

## Integration summary

- **Framework**: java
- **Project**: default
- **Environment**: default

## Instrumented events

| Event | Description |
| --- | --- |
| `User Signed Up` | Fires when a new user account is successfully registered via /auth/signup. |
| `User Logged In` | Fires when a user successfully authenticates via /auth/login. |
| `Login Failed` | Fires when a login attempt fails due to bad credentials or inactive account. |
| `Driver Created` | Fires when a new driver record is saved, including auto-generated driver number. |
| `Shift Created` | Fires when a new cab shift is created via POST /shifts. |
| `Shift Activated` | Fires when a shift transitions to active status via PUT /shifts/{id}/activate. |
| `Shift Deactivated` | Fires when a shift is deactivated via PUT /shifts/{id}/deactivate. |
| `Shift Ownership Transferred` | Fires when a shift's cab ownership is transferred to a different driver. |
| `Invoice Generated` | Fires when a new invoice is created for an account customer. |
| `Invoice Sent` | Fires when an invoice is marked as sent via PUT /invoices/{id}/send. |
| `Invoice Cancelled` | Fires when an invoice is cancelled via PUT /invoices/{id}/cancel. |
| `Invoice Emailed` | Fires when an invoice PDF is emailed to the customer. |
| `Credit Card Transactions Uploaded` | Fires when a credit card transaction CSV is successfully processed and saved. |
| `Airport Trips Uploaded` | Fires when an airport trip CSV is successfully uploaded and processed. |
| `Mileage Records Uploaded` | Fires when a mileage CSV is successfully uploaded and records are persisted. |
| `Taxi Caller Import Triggered` | Fires when a manual TaxiCaller API import is initiated by a user. |
| `Taxi Caller Import Completed` | Fires when a TaxiCaller scheduled or manual import finishes with success or error. |
| `EFT File Generated` | Fires when an EFT (Electronic Funds Transfer) file is generated for payroll. |
| `Report Downloaded` | Fires when any financial or driver report PDF is downloaded by a user. |
| `Lease Rate Override Created` | Fires when a driver- or cab-specific lease rate override is saved. |
| `Recurring Expense Created` | Fires when a new recurring expense (e.g. dispatch fee, insurance) is saved. |
| `One Time Expense Created` | Fires when a one-time expense is added to a shift or driver. |
| `Payment Recorded` | Fires when a payment is recorded against an account or invoice. |
| `Receipt Analysed` | Fires when an uploaded receipt is processed by the Gemini AI classifier. |
| `User Created By Admin` | Fires when an admin creates a new user account for another person. |

## Analytics dashboard

_The wizard didn't capture a dashboard URL. You can build one from your events at https://app.amplitude.com._

## Next steps

- Trigger the instrumented user flows in your app and confirm events appear in Amplitude.
- Set the Amplitude API key in your production environment (deploy platform settings or CI secrets).
- Re-run `npx @amplitude/wizard` if you want a richer end-of-run report — the agent writes a more detailed version when it reaches the conclude phase.

</wizard-report>
