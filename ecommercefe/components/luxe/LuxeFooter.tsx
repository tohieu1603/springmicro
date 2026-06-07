export default function Footer({ extended = false }: { extended?: boolean }) {
  return (
    <footer className="footer">
      <div className="footer-grid">
        <div>
          <h4>MAY WE HELP YOU?</h4>
          <ul>
            <li><a href="#">Contact Us</a></li>
            <li><a href="#">My Order</a></li>
            <li><a href="#">FAQs</a></li>
            <li><a href="#">Email Unsubscribe</a></li>
          </ul>
        </div>
        <div>
          <h4>THE COMPANY</h4>
          <ul>
            <li><a href="#">About Hieu</a></li>
            <li><a href="#">Hieu Equilibrium</a></li>
            <li><a href="#">Code of Ethics</a></li>
            <li><a href="#">Careers</a></li>
          </ul>
        </div>
        <div>
          <h4>STORE LOCATOR</h4>
          <div className="locator">
            <span>Country/Region, City</span>
            <span>&rsaquo;</span>
          </div>
          {extended && (
            <>
              <h4>SIGN UP FOR HIEU UPDATES</h4>
              <p className="signup-body">
                By entering your email address below, you consent to receiving our newsletter
                with access to our latest collections, events and initiatives. More details on
                this are provided in our{" "}
                <a href="#" style={{ textDecoration: "underline", color: "#fff" }}>
                  Privacy Policy
                </a>
              </p>
            </>
          )}
        </div>
      </div>
    </footer>
  );
}
