"use client";

import { makeBg } from "@/lib/illustrations";

import { useCheckout } from "./hooks/useCheckout";
import {
  AddressPicker,
  CartLines,
  EmptyState,
  OrderSummary,
  PaymentPicker,
  ShippingPicker,
} from "./components";

/**
 * Checkout container. Pulls every piece of state + action from useCheckout()
 * and prop-drills into dumb sub-components. Swap any sub-component without
 * touching cart/voucher/address/payment/shipping logic.
 */
export default function CheckoutPage() {
  const vm = useCheckout();
  const items = vm.cart?.items ?? [];
  const paymentName = vm.paymentMethods.find((m) => m.code === vm.paymentMethod)?.name ?? vm.paymentMethod;

  return (
    <>
      <section className="co-hero">
        <div
          className="co-bg"
          style={{ backgroundImage: makeBg("hobo", 1), backgroundSize: "cover" }}
        />
        <div className="co-title">SHOPPING BAG</div>
        <div className="co-logo">H</div>
      </section>

      <section className="co-section">
        <div className="co-grid">
          <div className="co-selections">
            <h2>
              YOUR SELECTIONS
              <span className="print">
                <svg viewBox="0 0 24 24" style={{ width: 14, height: 14, stroke: "#555", fill: "none", strokeWidth: 1.4 }}>
                  <path d="M6 9V4h12v5M6 14H4a2 2 0 01-2-2v-3a2 2 0 012-2h16a2 2 0 012 2v3a2 2 0 01-2 2h-2M6 14h12v6H6z" />
                </svg>
                Print
              </span>
            </h2>

            {(vm.phase === "loading" || vm.phase === "auth" || vm.phase === "empty") && (
              <EmptyState phase={vm.phase} />
            )}

            {vm.phase === "ready" && (
              <>
                <CartLines
                  items={items}
                  busyVariant={vm.busyVariant}
                  warnings={vm.cart?.warnings ?? []}
                  onChangeQty={vm.changeQty}
                  onRemove={vm.remove}
                  onClearAll={vm.clearAll}
                />

                <AddressPicker
                  addresses={vm.addresses}
                  selectedId={vm.selectedAddressId}
                  onSelect={vm.setSelectedAddressId}
                  onReload={vm.reloadAddresses}
                />

                <ShippingPicker
                  quote={vm.shipping}
                  loading={vm.shippingLoading}
                  hasAddress={!!vm.selectedAddress}
                />

                <PaymentPicker
                  methods={vm.paymentMethods}
                  selected={vm.paymentMethod}
                  onSelect={vm.setPaymentMethod}
                />
              </>
            )}
          </div>

          <OrderSummary
            userId={vm.cart?.userId}
            subtotal={vm.subtotal}
            discount={vm.discount}
            shippingFee={vm.shippingFee}
            total={vm.total}
            itemCount={items.length}
            placing={vm.placing}
            paymentMethod={vm.paymentMethod}
            paymentName={paymentName}
            shipping={vm.shipping}
            hasAddress={!!vm.selectedAddress}
            onCheckout={vm.placeOrder}
            voucherCode={vm.voucherCode}
            setVoucherCode={vm.setVoucherCode}
            voucherChecking={vm.voucherChecking}
            applied={vm.applied}
            voucherError={vm.voucherError}
            onApply={vm.applyVoucher}
            onClearVoucher={vm.clearVoucher}
          />
        </div>
      </section>

      <section className="shop-experience">
        <h2>Hieu Shopping Experience</h2>
        <div className="links">
          <a href="#">Authentic Hieu Guarantee</a>
          <span>|</span>
          <a href="#">The Finishing Touch</a>
          <span>|</span>
          <a href="/returns">Returns &amp; Exchanges</a>
        </div>
        <p>
          Hieu recognizes the importance of protecting the privacy of personal and
          financial information to ensure a safe and secure shopping experience.
          <br />
          <a href="#">Read our privacy policy</a>
        </p>
      </section>
    </>
  );
}
